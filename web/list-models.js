import { GoogleGenerativeAI } from '@google/generative-ai';
import fs from 'fs';

const apiKey = process.env.GEMINI_API_KEY || 'AIzaSyCUm2yx47nDZ_3ArOBOa0ZbT7-kxYlwaJU';

async function run() {
    try {
        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
        const data = await response.json();
        let text = "ALL MODELS:\n";
        if (data.models) {
            data.models.forEach(m => {
                text += m.name + '\n';
            });
        } else {
            text += JSON.stringify(data);
        }
        fs.writeFileSync('models.txt', text);
        console.log("WROTE TO models.txt");
    } catch (error) {
        console.error(error);
    }
}

run();
