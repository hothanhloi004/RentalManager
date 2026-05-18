package com.example.rentalmanager.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.rentalmanager.data.model.BillWithInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Xuất hóa đơn tiền trọ ra file PDF và mở/chia sẻ.
 */
public class InvoicePdfHelper {

    public static void exportAndShare(Context ctx, BillWithInfo bill,
                                      double ePrice, double wPrice) {
        try {
            File pdfFile = createPdf(ctx, bill, ePrice, wPrice);
            Uri uri = FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".provider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Hóa đơn tháng " + bill.month + " – " + bill.roomName);
            intent.putExtra(Intent.EXTRA_TEXT, "Hóa đơn tiền trọ tháng " + bill.month
                    + " cho phòng " + bill.roomName + " – " + bill.tenantName);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(intent, "Gửi hóa đơn PDF qua..."));

        } catch (Exception e) {
            Toast.makeText(ctx, "Lỗi tạo PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private static File createPdf(Context ctx, BillWithInfo bill,
                                   double ePrice, double wPrice) throws IOException {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

        int eUsed = bill.newElectric - bill.oldElectric;
        int wUsed = bill.newWater - bill.oldWater;
        double eMoney = eUsed * ePrice;
        double wMoney = wUsed * wPrice;
        double rent = Math.max(bill.totalAmount - eMoney - wMoney - bill.serviceFee, 0);
        boolean isPaid = "DA_THANH_TOAN".equals(bill.paymentStatus);

        // PDF page size A5 (148 x 210 mm -> 559 x 794 px at 96dpi)
        int pageW = 559, pageH = 794;

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, pageW, pageH, bgPaint);

        // Header bar
        Paint headerPaint = new Paint();
        headerPaint.setColor(0xFF4F46E5); // indigo
        canvas.drawRect(0, 0, pageW, 60, headerPaint);

        // Title
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextSize(20f);
        canvas.drawText("HÓA ĐƠN TIỀN TRỌ", 20, 38, titlePaint);

        // Month tag top right
        titlePaint.setTextSize(13f);
        String monthLabel = "Tháng " + bill.month;
        float mW = titlePaint.measureText(monthLabel);
        canvas.drawText(monthLabel, pageW - mW - 16, 38, titlePaint);

        // Room + tenant info block
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF6363A3);
        labelPaint.setTextSize(11f);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(0xFF1E1B4B);
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        valuePaint.setTextSize(13f);

        int y = 80;
        drawInfoRow(canvas, "Phòng", bill.roomName == null ? "" : bill.roomName, 20, y, labelPaint, valuePaint);
        y += 28;
        drawInfoRow(canvas, "Khách thuê", bill.tenantName == null ? "" : bill.tenantName, 20, y, labelPaint, valuePaint);
        y += 28;
        drawInfoRow(canvas, "Trạng thái", isPaid ? "✓ Đã thanh toán" : "⚠ Chưa thanh toán", 20, y, labelPaint, valuePaint);
        if (!isPaid) {
            // Recolor status
            Paint warnPaint = new Paint(valuePaint);
            warnPaint.setColor(0xFFDC2626);
            canvas.drawText("⚠ Chưa thanh toán",
                    20 + labelPaint.measureText("Trạng thái") + 8,
                    y + 13, warnPaint);
        }

        // Divider
        y += 20;
        Paint divPaint = new Paint();
        divPaint.setColor(0xFFE0E7FF);
        canvas.drawRect(20, y, pageW - 20, y + 1, divPaint);
        y += 14;

        // Title "Chi tiết"
        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(0xFF4F46E5);
        sectionPaint.setTypeface(Typeface.DEFAULT_BOLD);
        sectionPaint.setTextSize(13f);
        canvas.drawText("CHI TIẾT HÓA ĐƠN", 20, y, sectionPaint);
        y += 20;

        // Table rows
        drawTableRow(canvas, "Tiền phòng", fmt.format(rent) + " đ", pageW, y);        y += 32;
        drawTableRow(canvas, "Điện (" + eUsed + " kWh × " + fmt.format(ePrice) + "đ)",
                fmt.format(eMoney) + " đ", pageW, y); y += 32;
        drawTableRow(canvas, "Nước (" + wUsed + " m³ × " + fmt.format(wPrice) + "đ)",
                fmt.format(wMoney) + " đ", pageW, y); y += 32;

        if (bill.serviceFee > 0) {
            drawTableRow(canvas, "Phí dịch vụ", fmt.format(bill.serviceFee) + " đ", pageW, y); y += 32;
        }

        // Total row
        y += 4;
        Paint totalBgPaint = new Paint();
        totalBgPaint.setColor(0xFFEEF2FF);
        canvas.drawRect(16, y - 18, pageW - 16, y + 22, totalBgPaint);

        Paint totalLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        totalLabelPaint.setColor(0xFF4F46E5);
        totalLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        totalLabelPaint.setTextSize(15f);
        canvas.drawText("TỔNG CỘNG", 24, y + 10, totalLabelPaint);

        Paint totalValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        totalValuePaint.setColor(0xFF059669);
        totalValuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        totalValuePaint.setTextSize(17f);
        String totalStr = fmt.format(bill.totalAmount) + " đ";
        float tvW = totalValuePaint.measureText(totalStr);
        canvas.drawText(totalStr, pageW - tvW - 24, y + 12, totalValuePaint);

        // Footer
        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(0xFFAAAAAA);
        footerPaint.setTextSize(10f);
        String dateStr = "Xuất lúc: " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date());
        canvas.drawText(dateStr, 20, pageH - 20, footerPaint);
        canvas.drawText("Rental Manager App", pageW - footerPaint.measureText("Rental Manager App") - 20, pageH - 20, footerPaint);

        doc.finishPage(page);

        // Save to cache/invoices/
        File outDir = new File(ctx.getCacheDir(), "invoices");
        outDir.mkdirs();
        String fname = "hoadon_" + (bill.roomName != null ? bill.roomName.replaceAll("\\s+", "_") : bill.billId)
                + "_" + bill.month.replace("-", "") + ".pdf";
        File outFile = new File(outDir, fname);
        FileOutputStream fos = new FileOutputStream(outFile);
        doc.writeTo(fos);
        fos.close();
        doc.close();
        return outFile;
    }

    private static void drawInfoRow(Canvas c, String label, String value,
                                     int x, int y, Paint lp, Paint vp) {
        c.drawText(label + ":", x, y + 14, lp);
        c.drawText(value, x + lp.measureText(label + ":") + 6, y + 14, vp);
    }

    private static void drawTableRow(Canvas c, String label, String value, int pageW, int y) {
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setColor(0xFF374151);
        lp.setTextSize(12f);

        Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
        vp.setColor(0xFF111827);
        vp.setTypeface(Typeface.DEFAULT_BOLD);
        vp.setTextSize(12f);

        c.drawText(label, 28, y + 12, lp);
        float vW = vp.measureText(value);
        c.drawText(value, pageW - vW - 28, y + 12, vp);

        // Row divider
        Paint divP = new Paint();
        divP.setColor(0xFFF3F4F6);
        c.drawRect(20, y + 18, pageW - 20, y + 19, divP);
    }
}
