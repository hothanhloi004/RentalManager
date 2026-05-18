package com.example.rentalmanager.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.rentalmanager.data.dao.AssetDao;
import com.example.rentalmanager.data.dao.BillDao;
import com.example.rentalmanager.data.dao.ContractDao;
import com.example.rentalmanager.data.dao.RoomDao;
import com.example.rentalmanager.data.dao.SettingDao;
import com.example.rentalmanager.data.dao.TenantDao;
import com.example.rentalmanager.data.dao.PaymentDao;
import com.example.rentalmanager.data.entity.AssetEntity;
import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.entity.ContractEntity;
import com.example.rentalmanager.data.entity.PaymentEntity;
import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.entity.SettingEntity;
import com.example.rentalmanager.data.entity.TenantEntity;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;

@Database(
        entities = {
                RoomEntity.class,
                TenantEntity.class,
                ContractEntity.class,
                BillEntity.class,
                SettingEntity.class,
                PaymentEntity.class,
                AssetEntity.class
        },
        version = 12,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract RoomDao roomDao();
    public abstract TenantDao tenantDao();
    public abstract ContractDao contractDao();
    public abstract BillDao billDao();
    public abstract PaymentDao paymentDao();
    public abstract SettingDao settingDao();
    public abstract AssetDao assetDao();

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE settings ADD COLUMN hostelName TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE settings ADD COLUMN landlordName TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE settings ADD COLUMN hostelAddress TEXT DEFAULT ''");
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE settings ADD COLUMN landlordPhone TEXT DEFAULT ''");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "rental_manager_db"
                            )
                            .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}