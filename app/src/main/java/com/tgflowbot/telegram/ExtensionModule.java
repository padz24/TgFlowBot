package com.tgflowbot.telegram;

import com.tgflowbot.model.NodeType;

import java.util.ArrayList;
import java.util.List;

public class ExtensionModule {
    public String name;
    public String packageId;
    public List<TelegramMethod> methods;
    public String description;
    public String installer;
    public String version;
    public String category;

    public ExtensionModule() {
        methods = new ArrayList<>();
    }

    public ExtensionModule(String name, String packageId) {
        this.name = name;
        this.packageId = packageId;
        this.methods = new ArrayList<>();
    }

    public ExtensionModule(String name, String packageId, String description, String installer, String version, String category) {
        this.name = name;
        this.packageId = packageId;
        this.description = description;
        this.installer = installer;
        this.version = version;
        this.category = category;
        this.methods = new ArrayList<>();
    }

    public void addMethod(TelegramMethod m) {
        methods.add(m);
    }

    private static ExtensionModule pack(String name, String pkg, String desc, String cat, Object[][] methods) {
        ExtensionModule ext = new ExtensionModule(name, pkg, desc, "TgFlowBot", "1.0.0", cat);
        for (Object[] m : methods) {
            String apiName = (String) m[0];
            String displayName = (String) m[1];
            String description = (String) m[2];
            NodeType nodeType = (NodeType) m[3];
            ext.addMethod(new TelegramMethod(apiName, displayName, description, nodeType));
        }
        return ext;
    }

    public static List<ExtensionModule> getMarketplaceExtensions() {
        List<ExtensionModule> list = new ArrayList<>();

        list.add(pack("Telegram Core", "com.tgflowbot.ext.core",
                "Essential Telegram messaging: send, edit, delete messages, forward, copy, get chat info, and more.",
                "Communication",
                new Object[][]{
                    {"sendMessage", "Send Message", "Kirim pesan teks", NodeType.ACTION},
                    {"sendChatAction", "Send Action", "Kirim status typing", NodeType.ACTION},
                    {"forwardMessage", "Forward Message", "Forward pesan", NodeType.ACTION},
                    {"copyMessage", "Copy Message", "Copy pesan", NodeType.ACTION},
                    {"deleteMessage", "Delete Message", "Hapus pesan", NodeType.ACTION},
                    {"editMessageText", "Edit Message", "Edit teks pesan", NodeType.ACTION},
                    {"getMe", "Get Bot Info", "Info bot sendiri", NodeType.ACTION},
                    {"getChat", "Get Chat Info", "Info chat", NodeType.ACTION},
                    {"leaveChat", "Leave Chat", "Tinggalkan chat", NodeType.ACTION},
                    {"answerCallbackQuery", "Answer Callback", "Balas callback query", NodeType.ACTION},
                    {"answerChatJoinRequestQuery", "Approve Join", "Setujui/tolak join request", NodeType.ACTION},
                }));

        list.add(pack("Admin Tools", "com.tgflowbot.ext.admin",
                "Manage chat members: ban, unban, promote, restrict, pin messages, admin list, and more.",
                "Moderation",
                new Object[][]{
                    {"banChatMember", "Ban User", "Blokir anggota chat", NodeType.ACTION},
                    {"unbanChatMember", "Unban User", "Buka blokir anggota", NodeType.ACTION},
                    {"promoteChatMember", "Promote Admin", "Jadikan admin", NodeType.ACTION},
                    {"restrictChatMember", "Restrict User", "Batasi anggota", NodeType.ACTION},
                    {"pinChatMessage", "Pin Message", "Sematkan pesan", NodeType.ACTION},
                    {"unpinChatMessage", "Unpin Message", "Lepas sematan", NodeType.ACTION},
                    {"getChatAdministrators", "Get Admins", "Daftar admin chat", NodeType.ACTION},
                    {"getChatMembersCount", "Member Count", "Jumlah anggota", NodeType.ACTION},
                    {"getChatMember", "Get Member", "Info anggota chat", NodeType.ACTION},
                }));

        list.add(pack("Media Sender", "com.tgflowbot.ext.media",
                "Send all types of media: photos, videos, documents, audio, voice, animations, polls, dice, and location.",
                "Communication",
                new Object[][]{
                    {"sendPhoto", "Send Photo", "Kirim foto", NodeType.ACTION},
                    {"sendVideo", "Send Video", "Kirim video", NodeType.ACTION},
                    {"sendDocument", "Send Document", "Kirim file/dokumen", NodeType.ACTION},
                    {"sendAudio", "Send Audio", "Kirim file audio", NodeType.ACTION},
                    {"sendVoice", "Send Voice", "Kirim voice message", NodeType.ACTION},
                    {"sendAnimation", "Send Animation", "Kirim animasi GIF", NodeType.ACTION},
                    {"sendLocation", "Send Location", "Kirim lokasi", NodeType.ACTION},
                    {"sendVenue", "Send Venue", "Kirim tempat", NodeType.ACTION},
                    {"sendContact", "Send Contact", "Kirim kontak", NodeType.ACTION},
                    {"sendPoll", "Send Poll", "Kirim polling", NodeType.ACTION},
                    {"sendDice", "Send Dice", "Kirim dadu/emoji acak", NodeType.ACTION},
                }));

        list.add(pack("Phone Controls", "com.tgflowbot.ext.phone",
                "Control phone hardware: flashlight, vibrate, battery info, volume, brightness, clipboard, TTS, STT, and more.",
                "Device",
                new Object[][]{
                    {"_phone_flashlight", "Flashlight", "Nyalakan/matikan senter", NodeType.ACTION},
                    {"_phone_vibrate", "Vibrate", "Getar ponsel", NodeType.ACTION},
                    {"_phone_toast", "Toast", "Tampilkan pesan di layar", NodeType.ACTION},
                    {"_phone_battery", "Battery", "Dapatkan info baterai", NodeType.ACTION},
                    {"_phone_device_info", "Device Info", "Info perangkat", NodeType.ACTION},
                    {"_phone_open_url", "Open URL", "Buka URL di browser", NodeType.ACTION},
                    {"_phone_clipboard_set", "Clipboard Set", "Salin teks ke clipboard", NodeType.ACTION},
                    {"_phone_volume", "Volume", "Atur volume media 0-100", NodeType.ACTION},
                    {"_phone_brightness", "Brightness", "Atur kecerahan layar 0-255", NodeType.ACTION},
                    {"_phone_tts", "Text to Speech", "Ucapkan teks dengan TTS", NodeType.ACTION},
                    {"_phone_stt", "Speech to Text", "Dengar suara dan ubah ke teks", NodeType.ACTION},
                }));

        list.add(pack("File Operations", "com.tgflowbot.ext.file",
                "Read, write, append, delete, and list files on the device storage.",
                "Data",
                new Object[][]{
                    {"_file_read", "File Read", "Baca isi file", NodeType.ACTION},
                    {"_file_write", "File Write", "Tulis teks ke file", NodeType.ACTION},
                    {"_file_append", "File Append", "Tambah teks ke akhir file", NodeType.ACTION},
                    {"_file_delete", "File Delete", "Hapus file", NodeType.ACTION},
                    {"_file_exists", "File Exists", "Cek apakah file ada", NodeType.ACTION},
                    {"_file_list", "File List", "Daftar file dalam direktori", NodeType.ACTION},
                }));

        list.add(pack("Math & Logic", "com.tgflowbot.ext.math",
                "Mathematical operations: add, subtract, multiply, divide, power, sqrt, round, random, min, max, clamp, and more.",
                "Utility",
                new Object[][]{
                    {"_add", "Add", "Penjumlahan a + b", NodeType.ACTION},
                    {"_subtract", "Subtract", "Pengurangan a - b", NodeType.ACTION},
                    {"_multiply", "Multiply", "Perkalian a * b", NodeType.ACTION},
                    {"_divide", "Divide", "Pembagian a / b", NodeType.ACTION},
                    {"_modulo", "Modulo", "Sisa bagi a % b", NodeType.ACTION},
                    {"_power", "Power", "a pangkat b", NodeType.ACTION},
                    {"_sqrt", "Square Root", "Akar kuadrat", NodeType.ACTION},
                    {"_abs", "Absolute", "Nilai absolut", NodeType.ACTION},
                    {"_round", "Round", "Pembulatan ke integer terdekat", NodeType.ACTION},
                    {"_floor", "Floor", "Pembulatan ke bawah", NodeType.ACTION},
                    {"_ceil", "Ceil", "Pembulatan ke atas", NodeType.ACTION},
                    {"_min", "Min", "Nilai terkecil dari dua angka", NodeType.ACTION},
                    {"_max", "Max", "Nilai terbesar dari dua angka", NodeType.ACTION},
                    {"_clamp", "Clamp", "Batasi nilai antara min dan max", NodeType.ACTION},
                    {"_random", "Random Number", "Angka acak antara min-max", NodeType.ACTION},
                }));

        list.add(pack("Variables & Lists", "com.tgflowbot.ext.varlist",
                "Store, retrieve, and manipulate variables and lists throughout your workflow.",
                "Data",
                new Object[][]{
                    {"_set_variable", "Set Variable", "Simpan nilai ke variabel", NodeType.ACTION},
                    {"_get_variable", "Get Variable", "Ambil nilai dari variabel", NodeType.ACTION},
                    {"_var_add", "Var Add", "Tambah angka ke variable", NodeType.ACTION},
                    {"_var_subtract", "Var Subtract", "Kurang angka dari variable", NodeType.ACTION},
                    {"_var_multiply", "Var Multiply", "Kali variable dengan angka", NodeType.ACTION},
                    {"_var_divide", "Var Divide", "Bagi variable dengan angka", NodeType.ACTION},
                    {"_var_append", "Var Append", "Gabung teks ke variable", NodeType.ACTION},
                    {"_var_clear", "Var Clear", "Hapus isi variable", NodeType.ACTION},
                    {"_var_delete", "Var Delete", "Hapus variable", NodeType.ACTION},
                    {"_list_create", "List Create", "Buat list dari teks", NodeType.ACTION},
                    {"_list_add", "List Add", "Tambah item ke list", NodeType.ACTION},
                    {"_list_remove", "List Remove", "Hapus item dari list", NodeType.ACTION},
                    {"_list_get", "List Get", "Ambil item dari list", NodeType.ACTION},
                    {"_list_size", "List Size", "Jumlah item dalam list", NodeType.ACTION},
                    {"_list_clear", "List Clear", "Kosongkan list", NodeType.ACTION},
                    {"_list_join", "List Join", "Gabung item list", NodeType.ACTION},
                    {"_list_shuffle", "List Shuffle", "Acak urutan list", NodeType.ACTION},
                }));

        list.add(pack("AI Chat", "com.tgflowbot.ext.ai",
                "Chat with AI providers (OpenAI, Claude, Gemini, Groq, LLamaCPP) and use AI phone tools.",
                "Intelligence",
                new Object[][]{
                    {"ai_chat", "AI Chat", "Chat dengan AI", NodeType.ACTION},
                }));

        list.add(pack("HTTP & Web", "com.tgflowbot.ext.http",
                "Make HTTP requests to REST APIs and integrate with web services.",
                "Integration",
                new Object[][]{
                    {"_http_request", "HTTP Request", "Kirim request HTTP", NodeType.ACTION},
                }));

        list.add(pack("Text Processing", "com.tgflowbot.ext.text",
                "String manipulation: append, replace, and transform text in your workflow.",
                "Utility",
                new Object[][]{
                    {"_text_append", "Text Append", "Gabung dua teks", NodeType.ACTION},
                    {"_text_replace", "Text Replace", "Cari dan ganti teks", NodeType.ACTION},
                }));

        list.add(pack("Flow Control", "com.tgflowbot.ext.flow",
                "Control workflow execution: delay, repeat, loop break, wait until, return, and log messages.",
                "Utility",
                new Object[][]{
                    {"_delay", "Delay", "Tunggu sebelum lanjut (ms)", NodeType.ACTION},
                    {"_repeat", "Repeat", "Ulangi aliran N kali", NodeType.ACTION},
                    {"_wait_until", "Wait Until", "Tunggu sampai kondisi terpenuhi", NodeType.ACTION},
                    {"_loop_break", "Loop Break", "Hentikan perulangan", NodeType.ACTION},
                    {"_return", "Return", "Hentikan flow", NodeType.ACTION},
                    {"_log", "Log Message", "Tulis pesan ke log", NodeType.ACTION},
                }));

        list.add(pack("Triggers", "com.tgflowbot.ext.triggers",
                "Workflow triggers: incoming messages, scheduled cron, intervals, HTTP polling, webhooks, voice commands, and manual triggers.",
                "Flow",
                new Object[][]{
                    {"getUpdates", "On Message", "Trigger saat ada pesan baru", NodeType.TRIGGER},
                    {"setWebhook", "On Webhook", "Trigger via webhook URL", NodeType.TRIGGER},
                    {"_on_listening", "On Listening", "Trigger saat mendengar suara", NodeType.TRIGGER},
                    {"_on_schedule", "On Schedule (Cron)", "Trigger berdasarkan jadwal cron", NodeType.TRIGGER},
                    {"_on_interval", "On Interval", "Trigger setiap interval detik", NodeType.TRIGGER},
                    {"_on_http_poll", "On HTTP Poll", "Poll HTTP endpoint berkala", NodeType.TRIGGER},
                    {"_on_webhook", "On Webhook", "Trigger via HTTP (port 8080)", NodeType.TRIGGER},
                    {"_on_manual", "Manual Trigger", "Trigger manual dari UI", NodeType.TRIGGER},
                }));

        list.add(pack("Conditions & Outputs", "com.tgflowbot.ext.conditions",
                "Flow conditions (contains, equals, regex, compare) and output nodes (reply, forward, log to console).",
                "Flow",
                new Object[][]{
                    {"contains", "Contains", "Cek apakah teks mengandung kata", NodeType.CONDITION},
                    {"equals", "Equals", "Cek apakah teks sama persis", NodeType.CONDITION},
                    {"startsWith", "Starts With", "Cek awalan teks", NodeType.CONDITION},
                    {"matches", "Regex Match", "Cocokkan regex", NodeType.CONDITION},
                    {"hasMedia", "Has Media", "Cek apakah ada media", NodeType.CONDITION},
                    {"chatType", "Chat Type", "Cek tipe chat", NodeType.CONDITION},
                    {"alwaysTrue", "Always True", "Selalu true", NodeType.CONDITION},
                    {"alwaysFalse", "Always False", "Selalu false", NodeType.CONDITION},
                    {"_compare", "Compare", "Bandingkan dua angka", NodeType.CONDITION},
                    {"reply", "Reply", "Balas pesan", NodeType.OUTPUT},
                    {"forward", "Forward", "Forward ke chat lain", NodeType.OUTPUT},
                    {"log", "Log", "Catat ke log lokal", NodeType.OUTPUT},
                }));

        return list;
    }
}
