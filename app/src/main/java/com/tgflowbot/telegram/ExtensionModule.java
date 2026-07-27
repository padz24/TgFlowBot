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

    // Shorthand helpers for building ParamDef entries below.
    private static ParamDef p(String name, ParamDef.ParamType type, boolean required, String hint) {
        return new ParamDef(name, type, required, hint);
    }

    private static ParamDef p(String name, ParamDef.ParamType type, boolean required, String hint, String def) {
        return new ParamDef(name, type, required, hint, def);
    }

    private static ExtensionModule pack(String name, String pkg, String desc, String cat, Object[][] methods) {
        ExtensionModule ext = new ExtensionModule(name, pkg, desc, "TgFlowBot", "1.0.0", cat);
        for (Object[] m : methods) {
            String apiName = (String) m[0];
            String displayName = (String) m[1];
            String description = (String) m[2];
            NodeType nodeType = (NodeType) m[3];
            TelegramMethod tm = new TelegramMethod(apiName, displayName, description, nodeType);
            if (m.length > 4 && m[4] != null) {
                for (ParamDef pd : (ParamDef[]) m[4]) {
                    tm.addParam(pd);
                }
            }
            ext.addMethod(tm);
        }
        return ext;
    }

    public static List<ExtensionModule> getMarketplaceExtensions() {
        List<ExtensionModule> list = new ArrayList<>();

        list.add(pack("Telegram Core", "com.tgflowbot.ext.core",
                "Essential Telegram messaging: send, edit, delete, forward, copy messages, media groups, reactions, and chat info.",
                "Communication",
                new Object[][]{
                    {"sendMessage", "Send Message", "Kirim pesan teks", NodeType.ACTION, new ParamDef[]{
                        p("text", ParamDef.ParamType.STRING, true, "Isi pesan"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format: HTML/Markdown/MarkdownV2"),
                        p("link_preview_is_disabled", ParamDef.ParamType.BOOLEAN, false, "Nonaktifkan preview link"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa suara notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard atau force reply"),
                    }},
                    {"sendChatAction", "Send Action", "Kirim status typing/upload/record", NodeType.ACTION, new ParamDef[]{
                        p("action", ParamDef.ParamType.STRING, true, "typing/upload_photo/record_video/record_voice/upload_document/find_location/record_video_note/upload_video_note/choose_sticker", "typing"),
                    }},
                    {"forwardMessage", "Forward Message", "Forward pesan", NodeType.ACTION, new ParamDef[]{
                        p("from_chat_id", ParamDef.ParamType.STRING, true, "Chat ID asal pesan"),
                        p("message_id", ParamDef.ParamType.INTEGER, true, "ID pesan yang diforward"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                    }},
                    {"forwardMessages", "Forward Messages", "Forward beberapa pesan sekaligus", NodeType.ACTION, new ParamDef[]{
                        p("from_chat_id", ParamDef.ParamType.STRING, true, "Chat ID asal"),
                        p("message_ids", ParamDef.ParamType.STRING, true, "JSON array ID pesan, mis. [1,2,3]"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                    }},
                    {"copyMessage", "Copy Message", "Copy pesan tanpa forward", NodeType.ACTION, new ParamDef[]{
                        p("from_chat_id", ParamDef.ParamType.STRING, true, "Chat ID asal pesan"),
                        p("message_id", ParamDef.ParamType.INTEGER, true, "ID pesan yang dicopy"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan baru (ganti original)"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendMediaGroup", "Send Media Group", "Kirim grup media (foto/video)", NodeType.ACTION, new ParamDef[]{
                        p("media", ParamDef.ParamType.STRING, true, "JSON array media, mis. [{\"type\":\"photo\",\"media\":\"url\"}]"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                    }},
                    {"deleteMessage", "Delete Message", "Hapus pesan", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, true, "ID pesan yang dihapus"),
                    }},
                    {"editMessageText", "Edit Message Text", "Edit teks pesan", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan (inline: kosongkan)"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message (jika inline)"),
                        p("text", ParamDef.ParamType.STRING, true, "Teks baru"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format: HTML/Markdown/MarkdownV2"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard baru"),
                    }},
                    {"editMessageCaption", "Edit Message Caption", "Edit caption media", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                        p("caption", ParamDef.ParamType.STRING, false, "Caption baru"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard baru"),
                    }},
                    {"editMessageMedia", "Edit Message Media", "Edit media di pesan", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                        p("media", ParamDef.ParamType.STRING, true, "JSON media object, mis. {\"type\":\"photo\",\"media\":\"url\"}"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard baru"),
                    }},
                    {"editMessageReplyMarkup", "Edit Reply Markup", "Edit tombol inline", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard baru"),
                    }},
                    {"setMessageReaction", "Set Reaction", "Beri reaksi ke pesan", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, true, "ID pesan"),
                        p("reaction", ParamDef.ParamType.STRING, false, "JSON array reaksi, mis. [{\"type\":\"emoji\",\"emoji\":\"👍\"}]"),
                        p("is_big", ParamDef.ParamType.BOOLEAN, false, "Tampilkan reaksi besar"),
                    }},
                    {"getUpdates", "Get Updates (Action)", "Ambil pesan baru secara manual", NodeType.ACTION, new ParamDef[]{
                        p("offset", ParamDef.ParamType.INTEGER, false, "ID update pertama yang diambil"),
                        p("limit", ParamDef.ParamType.INTEGER, false, "Maksimal update (1-100)", "100"),
                        p("timeout", ParamDef.ParamType.INTEGER, false, "Long polling timeout (detik)", "0"),
                    }},
                    {"getMe", "Get Bot Info", "Info bot sendiri", NodeType.ACTION, null},
                    {"getChat", "Get Chat Info", "Info chat/grup/channel", NodeType.ACTION, null},
                    {"leaveChat", "Leave Chat", "Tinggalkan chat/grup/channel", NodeType.ACTION, null},
                    {"answerCallbackQuery", "Answer Callback", "Balas callback query dari inline keyboard", NodeType.ACTION, new ParamDef[]{
                        p("callback_query_id", ParamDef.ParamType.STRING, true, "ID callback query"),
                        p("text", ParamDef.ParamType.STRING, false, "Teks notifikasi (toast/alert)"),
                        p("show_alert", ParamDef.ParamType.BOOLEAN, false, "Tampilkan sebagai alert"),
                        p("url", ParamDef.ParamType.STRING, false, "URL yang dibuka user"),
                        p("cache_time", ParamDef.ParamType.INTEGER, false, "Waktu cache (detik)"),
                    }},
                    {"getChatMemberCount", "Get Member Count", "Jumlah anggota chat", NodeType.ACTION, null},
                    {"getFile", "Get File Info", "Info file dari file_id untuk download", NodeType.ACTION, new ParamDef[]{
                        p("file_id", ParamDef.ParamType.STRING, true, "file_id dari Telegram"),
                    }},
                    {"getUserProfilePhotos", "Get User Photos", "Dapatkan foto profil user", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("offset", ParamDef.ParamType.INTEGER, false, "Offset foto (0-based)"),
                        p("limit", ParamDef.ParamType.INTEGER, false, "Maks jumlah (1-100)", "100"),
                    }},
                }));

        list.add(pack("Admin Tools", "com.tgflowbot.ext.admin",
                "Manage chat members, bans, promotions, restrictions, invites, pins, and chat settings.",
                "Moderation",
                new Object[][]{
                    {"banChatMember", "Ban User", "Blokir anggota dari chat", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user yang diban"),
                        p("until_date", ParamDef.ParamType.INTEGER, false, "Unix timestamp sampai kapan (0 = permanen)"),
                        p("revoke_messages", ParamDef.ParamType.BOOLEAN, false, "Hapus semua pesan user"),
                    }},
                    {"unbanChatMember", "Unban User", "Buka blokir anggota", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user yang diunban"),
                        p("only_if_banned", ParamDef.ParamType.BOOLEAN, false, "Unban hanya jika sedang diban"),
                    }},
                    {"banChatSenderChat", "Ban Channel", "Blokir channel pengirim", NodeType.ACTION, new ParamDef[]{
                        p("sender_chat_id", ParamDef.ParamType.STRING, true, "ID channel yang diblokir"),
                    }},
                    {"unbanChatSenderChat", "Unban Channel", "Buka blokir channel", NodeType.ACTION, new ParamDef[]{
                        p("sender_chat_id", ParamDef.ParamType.STRING, true, "ID channel yang dibuka"),
                    }},
                    {"promoteChatMember", "Promote Admin", "Jadikan/jabut admin", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("is_anonymous", ParamDef.ParamType.BOOLEAN, false, "Sembunyikan admin dari anggota"),
                        p("can_manage_chat", ParamDef.ParamType.BOOLEAN, false, "Izin kelola chat"),
                        p("can_delete_messages", ParamDef.ParamType.BOOLEAN, false, "Izin hapus pesan"),
                        p("can_restrict_members", ParamDef.ParamType.BOOLEAN, false, "Izin batasi anggota"),
                        p("can_pin_messages", ParamDef.ParamType.BOOLEAN, false, "Izin sematkan pesan"),
                        p("can_promote_members", ParamDef.ParamType.BOOLEAN, false, "Izin jadikan admin lain"),
                        p("can_change_info", ParamDef.ParamType.BOOLEAN, false, "Izin ubah info chat"),
                        p("can_invite_users", ParamDef.ParamType.BOOLEAN, false, "Izin undang anggota"),
                        p("can_post_messages", ParamDef.ParamType.BOOLEAN, false, "Izin kirim pesan (channel)"),
                        p("can_edit_messages", ParamDef.ParamType.BOOLEAN, false, "Izin edit pesan (channel)"),
                        p("can_post_stories", ParamDef.ParamType.BOOLEAN, false, "Izin posting story"),
                        p("can_edit_stories", ParamDef.ParamType.BOOLEAN, false, "Izin edit story"),
                        p("can_delete_stories", ParamDef.ParamType.BOOLEAN, false, "Izin hapus story"),
                        p("can_manage_video_chats", ParamDef.ParamType.BOOLEAN, false, "Izin kelola video chat"),
                        p("can_manage_topics", ParamDef.ParamType.BOOLEAN, false, "Izin kelola forum topics"),
                    }},
                    {"restrictChatMember", "Restrict User", "Batasi izin anggota", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("until_date", ParamDef.ParamType.INTEGER, false, "Unix timestamp sampai kapan (0 = permanen)"),
                        p("can_send_messages", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim pesan"),
                        p("can_send_audios", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim audio"),
                        p("can_send_documents", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim dokumen"),
                        p("can_send_photos", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim foto"),
                        p("can_send_videos", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim video"),
                        p("can_send_video_notes", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim video note"),
                        p("can_send_voice_notes", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim voice note"),
                        p("can_send_polls", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim poll"),
                        p("can_send_other_messages", ParamDef.ParamType.BOOLEAN, false, "Izinkan kirim pesan lain"),
                        p("can_add_web_page_previews", ParamDef.ParamType.BOOLEAN, false, "Izinkan preview link"),
                        p("can_change_info", ParamDef.ParamType.BOOLEAN, false, "Izinkan ubah info grup"),
                        p("can_invite_users", ParamDef.ParamType.BOOLEAN, false, "Izinkan undang anggota"),
                        p("can_pin_messages", ParamDef.ParamType.BOOLEAN, false, "Izinkan semat pesan"),
                        p("can_manage_topics", ParamDef.ParamType.BOOLEAN, false, "Izinkan kelola topics"),
                    }},
                    {"setChatPermissions", "Set Permissions", "Atur izin default chat", NodeType.ACTION, new ParamDef[]{
                        p("permissions", ParamDef.ParamType.STRING, true, "JSON ChatPermissions object"),
                        p("use_independent_chat_permissions", ParamDef.ParamType.BOOLEAN, false, "Izin independen per konten"),
                    }},
                    {"setChatAdministratorCustomTitle", "Set Admin Title", "Atur title khusus admin", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID admin"),
                        p("custom_title", ParamDef.ParamType.STRING, true, "Title khusus (max 16 karakter)"),
                    }},
                    {"setChatTitle", "Set Chat Title", "Ubah judul grup/channel", NodeType.ACTION, new ParamDef[]{
                        p("title", ParamDef.ParamType.STRING, true, "Judul baru (max 128 karakter)"),
                    }},
                    {"setChatDescription", "Set Description", "Ubah deskripsi chat", NodeType.ACTION, new ParamDef[]{
                        p("description", ParamDef.ParamType.STRING, false, "Deskripsi baru (max 255 karakter)"),
                    }},
                    {"setChatPhoto", "Set Photo", "Ubah foto chat", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("photo", ParamDef.ParamType.STRING, false, "URL foto (jika input_type=url)"),
                    }},
                    {"deleteChatPhoto", "Delete Photo", "Hapus foto chat", NodeType.ACTION, null},
                    {"setChatStickerSet", "Set Sticker Set", "Atur sticker set grup", NodeType.ACTION, new ParamDef[]{
                        p("sticker_set_name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                    }},
                    {"deleteChatStickerSet", "Delete Sticker Set", "Hapus sticker set grup", NodeType.ACTION, null},
                    {"pinChatMessage", "Pin Message", "Sematkan pesan di chat", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, true, "ID pesan yang disematkan"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Sematkan tanpa notifikasi"),
                    }},
                    {"unpinChatMessage", "Unpin Message", "Lepas sematan pesan", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "Kosongkan untuk unpin pesan tersemat terbaru"),
                    }},
                    {"unpinAllChatMessages", "Unpin All", "Lepas semua sematan", NodeType.ACTION, null},
                    {"getChatAdministrators", "Get Admins", "Daftar admin chat", NodeType.ACTION, null},
                    {"getChatMember", "Get Member", "Info anggota chat", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                    }},
                    {"approveChatJoinRequest", "Approve Join", "Setujui permintaan bergabung", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                    }},
                    {"declineChatJoinRequest", "Decline Join", "Tolak permintaan bergabung", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                    }},
                    {"exportChatInviteLink", "Export Invite Link", "Buat link undangan baru", NodeType.ACTION, null},
                    {"createChatInviteLink", "Create Invite Link", "Buat link undangan dengan opsi", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, false, "Nama link undangan"),
                        p("expire_date", ParamDef.ParamType.INTEGER, false, "Unix timestamp kadaluarsa"),
                        p("member_limit", ParamDef.ParamType.INTEGER, false, "Batas anggota (1-99999)"),
                        p("creates_join_request", ParamDef.ParamType.BOOLEAN, false, "Butuh persetujuan join"),
                    }},
                    {"editChatInviteLink", "Edit Invite Link", "Edit link undangan", NodeType.ACTION, new ParamDef[]{
                        p("invite_link", ParamDef.ParamType.STRING, true, "Link undangan yang diedit"),
                        p("name", ParamDef.ParamType.STRING, false, "Nama baru"),
                        p("expire_date", ParamDef.ParamType.INTEGER, false, "Unix timestamp kadaluarsa baru"),
                        p("member_limit", ParamDef.ParamType.INTEGER, false, "Batas anggota baru"),
                        p("creates_join_request", ParamDef.ParamType.BOOLEAN, false, "Butuh persetujuan join"),
                    }},
                    {"revokeChatInviteLink", "Revoke Invite Link", "Cabut link undangan", NodeType.ACTION, new ParamDef[]{
                        p("invite_link", ParamDef.ParamType.STRING, true, "Link undangan yang dicabut"),
                    }},
                    {"getUserChatBoosts", "Get User Boosts", "Dapatkan boosts user di chat", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                    }},
                }));

        list.add(pack("Media Sender", "com.tgflowbot.ext.media",
                "Send all types of media: photos, videos, documents, audio, voice, animations, polls, dice, location, venue, and more.",
                "Communication",
                new Object[][]{
                    {"sendPhoto", "Send Photo", "Kirim foto", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("photo", ParamDef.ParamType.STRING, false, "URL foto (jika input_type=url)"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan foto"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption: HTML/Markdown/MarkdownV2"),
                        p("has_spoiler", ParamDef.ParamType.BOOLEAN, false, "Tandai sebagai spoiler"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendVideo", "Send Video", "Kirim video", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("video", ParamDef.ParamType.STRING, false, "URL video (jika input_type=url)"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan video"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("duration", ParamDef.ParamType.INTEGER, false, "Durasi (detik)"),
                        p("width", ParamDef.ParamType.INTEGER, false, "Lebar video"),
                        p("height", ParamDef.ParamType.INTEGER, false, "Tinggi video"),
                        p("supports_streaming", ParamDef.ParamType.BOOLEAN, false, "Dukung streaming"),
                        p("has_spoiler", ParamDef.ParamType.BOOLEAN, false, "Tandai sebagai spoiler"),
                        p("thumbnail", ParamDef.ParamType.STRING, false, "URL thumbnail (jika upload)"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendDocument", "Send Document", "Kirim file/dokumen", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("document", ParamDef.ParamType.STRING, false, "URL dokumen (jika input_type=url)"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan dokumen"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("thumbnail", ParamDef.ParamType.STRING, false, "URL thumbnail"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendAudio", "Send Audio", "Kirim file audio", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("audio", ParamDef.ParamType.STRING, false, "URL audio (jika input_type=url)"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan audio"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("duration", ParamDef.ParamType.INTEGER, false, "Durasi (detik)"),
                        p("performer", ParamDef.ParamType.STRING, false, "Nama artis/pemain"),
                        p("title", ParamDef.ParamType.STRING, false, "Judul lagu"),
                        p("thumbnail", ParamDef.ParamType.STRING, false, "URL thumbnail"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendVoice", "Send Voice", "Kirim voice message", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("voice", ParamDef.ParamType.STRING, false, "URL voice (jika input_type=url)"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("duration", ParamDef.ParamType.INTEGER, false, "Durasi (detik)"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendVideoNote", "Send Video Note", "Kirim video note (lingkaran)", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("video_note", ParamDef.ParamType.STRING, false, "URL video note"),
                        p("duration", ParamDef.ParamType.INTEGER, false, "Durasi (detik)"),
                        p("length", ParamDef.ParamType.INTEGER, false, "Diameter video note"),
                        p("thumbnail", ParamDef.ParamType.STRING, false, "URL thumbnail"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendAnimation", "Send Animation", "Kirim animasi GIF", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("animation", ParamDef.ParamType.STRING, false, "URL GIF (jika input_type=url)"),
                        p("caption", ParamDef.ParamType.STRING, false, "Keterangan"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format caption"),
                        p("duration", ParamDef.ParamType.INTEGER, false, "Durasi (detik)"),
                        p("width", ParamDef.ParamType.INTEGER, false, "Lebar"),
                        p("height", ParamDef.ParamType.INTEGER, false, "Tinggi"),
                        p("thumbnail", ParamDef.ParamType.STRING, false, "URL thumbnail"),
                        p("has_spoiler", ParamDef.ParamType.BOOLEAN, false, "Tandai sebagai spoiler"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendLocation", "Send Location", "Kirim lokasi", NodeType.ACTION, new ParamDef[]{
                        p("latitude", ParamDef.ParamType.FLOAT, true, "Garis lintang"),
                        p("longitude", ParamDef.ParamType.FLOAT, true, "Garis bujur"),
                        p("live_period", ParamDef.ParamType.INTEGER, false, "Durasi live location (detik)"),
                        p("horizontal_accuracy", ParamDef.ParamType.FLOAT, false, "Akurasi horizontal (meter)"),
                        p("heading", ParamDef.ParamType.INTEGER, false, "Arah (1-360 derajat)"),
                        p("proximity_alert_radius", ParamDef.ParamType.INTEGER, false, "Radius peringatan (meter)"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendVenue", "Send Venue", "Kirim tempat", NodeType.ACTION, new ParamDef[]{
                        p("latitude", ParamDef.ParamType.FLOAT, true, "Garis lintang"),
                        p("longitude", ParamDef.ParamType.FLOAT, true, "Garis bujur"),
                        p("title", ParamDef.ParamType.STRING, true, "Nama tempat"),
                        p("address", ParamDef.ParamType.STRING, true, "Alamat tempat"),
                        p("foursquare_id", ParamDef.ParamType.STRING, false, "ID Foursquare"),
                        p("foursquare_type", ParamDef.ParamType.STRING, false, "Tipe Foursquare"),
                        p("google_place_id", ParamDef.ParamType.STRING, false, "ID Google Places"),
                        p("google_place_type", ParamDef.ParamType.STRING, false, "Tipe Google Places"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendContact", "Send Contact", "Kirim kontak", NodeType.ACTION, new ParamDef[]{
                        p("phone_number", ParamDef.ParamType.STRING, true, "Nomor telepon"),
                        p("first_name", ParamDef.ParamType.STRING, true, "Nama depan"),
                        p("last_name", ParamDef.ParamType.STRING, false, "Nama belakang"),
                        p("vcard", ParamDef.ParamType.STRING, false, "Kontak dalam format vCard"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendPoll", "Send Poll", "Kirim polling", NodeType.ACTION, new ParamDef[]{
                        p("question", ParamDef.ParamType.STRING, true, "Pertanyaan polling"),
                        p("options", ParamDef.ParamType.STRING, true, "Pilihan dalam JSON array, mis. [\"A\",\"B\"]"),
                        p("is_anonymous", ParamDef.ParamType.BOOLEAN, false, "Polling anonim", "true"),
                        p("type", ParamDef.ParamType.STRING, false, "regular atau quiz", "regular"),
                        p("allows_multiple_answers", ParamDef.ParamType.BOOLEAN, false, "Boleh pilih lebih dari satu"),
                        p("correct_option_id", ParamDef.ParamType.INTEGER, false, "Index jawaban benar (untuk quiz)"),
                        p("explanation", ParamDef.ParamType.STRING, false, "Penjelasan jawaban (untuk quiz)"),
                        p("explanation_parse_mode", ParamDef.ParamType.STRING, false, "Format penjelasan"),
                        p("open_period", ParamDef.ParamType.INTEGER, false, "Durasi poll terbuka (detik)"),
                        p("close_date", ParamDef.ParamType.INTEGER, false, "Unix timestamp poll ditutup"),
                        p("is_closed", ParamDef.ParamType.BOOLEAN, false, "Tutup poll segera"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"sendDice", "Send Dice", "Kirim dadu/emoji acak", NodeType.ACTION, new ParamDef[]{
                        p("emoji", ParamDef.ParamType.STRING, false, "\ud83c\udfb2 \ud83c\udfaf \ud83c\udfc0 \u26bd \ud83c\udfb3 \ud83c\udfb0", "\ud83c\udfb2"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"stopPoll", "Stop Poll", "Hentikan polling", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, true, "ID pesan polling"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"editMessageLiveLocation", "Edit Live Location", "Update live location", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                        p("latitude", ParamDef.ParamType.FLOAT, true, "Garis lintang baru"),
                        p("longitude", ParamDef.ParamType.FLOAT, true, "Garis bujur baru"),
                        p("live_period", ParamDef.ParamType.INTEGER, false, "Perpanjang durasi (detik)"),
                        p("horizontal_accuracy", ParamDef.ParamType.FLOAT, false, "Akurasi horizontal"),
                        p("heading", ParamDef.ParamType.INTEGER, false, "Arah (1-360)"),
                        p("proximity_alert_radius", ParamDef.ParamType.INTEGER, false, "Radius peringatan"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"stopMessageLiveLocation", "Stop Live Location", "Hentikan live location", NodeType.ACTION, new ParamDef[]{
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                }));

        list.add(pack("Stickers & Emoji", "com.tgflowbot.ext.stickers",
                "Send stickers, manage sticker sets, custom emoji stickers, and more.",
                "Communication",
                new Object[][]{
                    {"sendSticker", "Send Sticker", "Kirim sticker", NodeType.ACTION, new ParamDef[]{
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "url"),
                        p("sticker", ParamDef.ParamType.STRING, false, "URL/ID sticker (jika input_type=url)"),
                        p("emoji", ParamDef.ParamType.STRING, false, "Emoji terkait sticker"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"getStickerSet", "Get Sticker Set", "Info set sticker", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set (dengan @)"),
                    }},
                    {"getCustomEmojiStickers", "Get Emoji Stickers", "Info custom emoji stickers", NodeType.ACTION, new ParamDef[]{
                        p("custom_emoji_ids", ParamDef.ParamType.STRING, true, "JSON array ID custom emoji"),
                    }},
                    {"uploadStickerFile", "Upload Sticker", "Upload file sticker", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user pembuat"),
                        p("sticker_format", ParamDef.ParamType.STRING, true, "static/animated/video"),
                        p("input_type", ParamDef.ParamType.STRING, false, "url atau upload", "upload"),
                        p("sticker", ParamDef.ParamType.STRING, false, "File sticker"),
                    }},
                    {"createNewStickerSet", "Create Sticker Set", "Buat set sticker baru", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user pembuat"),
                        p("name", ParamDef.ParamType.STRING, true, "Nama set (dengan @bot, akhir _by_BotName)"),
                        p("title", ParamDef.ParamType.STRING, true, "Judul set sticker"),
                        p("stickers", ParamDef.ParamType.STRING, true, "JSON array InputSticker objects"),
                        p("sticker_format", ParamDef.ParamType.STRING, true, "static/animated/video"),
                        p("sticker_type", ParamDef.ParamType.STRING, false, "regular/mask/custom_emoji", "regular"),
                        p("needs_repainting", ParamDef.ParamType.BOOLEAN, false, "Sticker siap di-recolor"),
                    }},
                    {"addStickerToSet", "Add Sticker", "Tambah sticker ke set", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user pembuat"),
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                        p("sticker", ParamDef.ParamType.STRING, true, "JSON InputSticker object"),
                    }},
                    {"replaceStickerInSet", "Replace Sticker", "Ganti sticker di set", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user pembuat"),
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                        p("old_sticker", ParamDef.ParamType.STRING, true, "File_id sticker yang diganti"),
                        p("sticker", ParamDef.ParamType.STRING, true, "JSON InputSticker object baru"),
                    }},
                    {"setStickerPositionInSet", "Set Sticker Position", "Atur urutan sticker", NodeType.ACTION, new ParamDef[]{
                        p("sticker", ParamDef.ParamType.STRING, true, "File_id sticker"),
                        p("position", ParamDef.ParamType.INTEGER, true, "Posisi baru (0-based)"),
                    }},
                    {"deleteStickerFromSet", "Delete Sticker", "Hapus sticker dari set", NodeType.ACTION, new ParamDef[]{
                        p("sticker", ParamDef.ParamType.STRING, true, "File_id sticker yang dihapus"),
                    }},
                    {"setStickerEmojiList", "Set Sticker Emoji", "Atur emoji terkait sticker", NodeType.ACTION, new ParamDef[]{
                        p("sticker", ParamDef.ParamType.STRING, true, "File_id sticker"),
                        p("emoji_list", ParamDef.ParamType.STRING, true, "JSON array emoji, mis. [\"👍\",\"❤️\"]"),
                    }},
                    {"setStickerKeywords", "Set Sticker Keywords", "Atur kata kunci sticker", NodeType.ACTION, new ParamDef[]{
                        p("sticker", ParamDef.ParamType.STRING, true, "File_id sticker"),
                        p("keywords", ParamDef.ParamType.STRING, true, "JSON array keyword, mis. [\"kata1\",\"kata2\"]"),
                    }},
                    {"setStickerMaskPosition", "Set Sticker Mask", "Atur posisi mask sticker", NodeType.ACTION, new ParamDef[]{
                        p("sticker", ParamDef.ParamType.STRING, true, "File_id sticker"),
                        p("mask_position", ParamDef.ParamType.STRING, false, "JSON MaskPosition object"),
                    }},
                    {"setStickerSetTitle", "Set Set Title", "Atur judul sticker set", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                        p("title", ParamDef.ParamType.STRING, true, "Judul baru"),
                    }},
                    {"setStickerSetThumbnail", "Set Set Thumbnail", "Atur thumbnail sticker set", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user pembuat"),
                        p("format", ParamDef.ParamType.STRING, false, "static/animated/video", "static"),
                        p("thumbnail", ParamDef.ParamType.STRING, false, "URL/upload thumbnail (kosongkan = hapus)"),
                    }},
                    {"setCustomEmojiStickerSetThumbnail", "Set Emoji Thumbnail", "Atur thumbnail custom emoji set", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                        p("custom_emoji_id", ParamDef.ParamType.STRING, false, "ID custom emoji untuk thumbnail"),
                    }},
                    {"deleteStickerSet", "Delete Sticker Set", "Hapus seluruh sticker set", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama sticker set"),
                    }},
                }));

        list.add(pack("Forum Topics", "com.tgflowbot.ext.forum",
                "Manage forum topics: create, edit, close, reopen, hide, unpin, and more in forum supergroups.",
                "Moderation",
                new Object[][]{
                    {"createForumTopic", "Create Topic", "Buat topik forum baru", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama topik (1-128 karakter)"),
                        p("icon_color", ParamDef.ParamType.INTEGER, false, "Warna icon (0x6FB9F0 dll)"),
                        p("icon_custom_emoji_id", ParamDef.ParamType.STRING, false, "ID custom emoji sebagai icon"),
                    }},
                    {"editForumTopic", "Edit Topic", "Edit topik forum", NodeType.ACTION, new ParamDef[]{
                        p("message_thread_id", ParamDef.ParamType.INTEGER, true, "ID thread topik"),
                        p("name", ParamDef.ParamType.STRING, false, "Nama topik baru"),
                        p("icon_custom_emoji_id", ParamDef.ParamType.STRING, false, "ID custom emoji baru"),
                    }},
                    {"closeForumTopic", "Close Topic", "Tutup topik forum", NodeType.ACTION, new ParamDef[]{
                        p("message_thread_id", ParamDef.ParamType.INTEGER, true, "ID thread topik"),
                    }},
                    {"reopenForumTopic", "Reopen Topic", "Buka kembali topik forum", NodeType.ACTION, new ParamDef[]{
                        p("message_thread_id", ParamDef.ParamType.INTEGER, true, "ID thread topik"),
                    }},
                    {"deleteForumTopic", "Delete Topic", "Hapus topik forum", NodeType.ACTION, new ParamDef[]{
                        p("message_thread_id", ParamDef.ParamType.INTEGER, true, "ID thread topik"),
                    }},
                    {"unpinAllForumTopicMessages", "Unpin Topic Messages", "Lepas semua sematan di topik", NodeType.ACTION, new ParamDef[]{
                        p("message_thread_id", ParamDef.ParamType.INTEGER, true, "ID thread topik"),
                    }},
                    {"getForumTopicIconStickers", "Get Topic Icons", "Dapatkan sticker icon topik", NodeType.ACTION, null},
                    {"editGeneralForumTopic", "Edit General Topic", "Edit topik umum forum", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama baru topik umum"),
                    }},
                    {"closeGeneralForumTopic", "Close General Topic", "Tutup topik umum forum", NodeType.ACTION, null},
                    {"reopenGeneralForumTopic", "Reopen General Topic", "Buka topik umum forum", NodeType.ACTION, null},
                    {"hideGeneralForumTopic", "Hide General Topic", "Sembunyikan topik umum", NodeType.ACTION, null},
                    {"unhideGeneralForumTopic", "Unhide General Topic", "Tampilkan topik umum", NodeType.ACTION, null},
                    {"unpinAllGeneralForumTopicMessages", "Unpin General Messages", "Lepas semua sematan topik umum", NodeType.ACTION, null},
                }));

        list.add(pack("Bot Configuration", "com.tgflowbot.ext.botcfg",
                "Configure bot profile, commands, webhook, menu button, and admin rights.",
                "Utility",
                new Object[][]{
                    {"setMyCommands", "Set Commands", "Atur daftar perintah bot", NodeType.ACTION, new ParamDef[]{
                        p("commands", ParamDef.ParamType.STRING, true, "JSON array BotCommand, mis. [{\"command\":\"start\",\"description\":\"Mulai\"}]"),
                        p("scope", ParamDef.ParamType.STRING, false, "JSON BotCommandScope object"),
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa (mis. id)"),
                    }},
                    {"getMyCommands", "Get Commands", "Daftar perintah bot", NodeType.ACTION, new ParamDef[]{
                        p("scope", ParamDef.ParamType.STRING, false, "JSON BotCommandScope object"),
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"deleteMyCommands", "Delete Commands", "Hapus daftar perintah bot", NodeType.ACTION, new ParamDef[]{
                        p("scope", ParamDef.ParamType.STRING, false, "JSON BotCommandScope object"),
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"setMyName", "Set Bot Name", "Atur nama bot", NodeType.ACTION, new ParamDef[]{
                        p("name", ParamDef.ParamType.STRING, true, "Nama bot baru"),
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"getMyName", "Get Bot Name", "Dapatkan nama bot", NodeType.ACTION, new ParamDef[]{
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"setMyDescription", "Set Bot Description", "Atur deskripsi bot", NodeType.ACTION, new ParamDef[]{
                        p("description", ParamDef.ParamType.STRING, true, "Deskripsi bot (max 512 karakter)"),
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"getMyDescription", "Get Bot Description", "Dapatkan deskripsi bot", NodeType.ACTION, new ParamDef[]{
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"setMyShortDescription", "Set Short Description", "Atur deskripsi singkat bot", NodeType.ACTION, new ParamDef[]{
                        p("short_description", ParamDef.ParamType.STRING, true, "Deskripsi singkat (max 120 karakter)"),
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"getMyShortDescription", "Get Short Description", "Dapatkan deskripsi singkat bot", NodeType.ACTION, new ParamDef[]{
                        p("language_code", ParamDef.ParamType.STRING, false, "Kode bahasa"),
                    }},
                    {"setMyDefaultAdministratorRights", "Set Default Rights", "Atur default hak admin bot", NodeType.ACTION, new ParamDef[]{
                        p("rights", ParamDef.ParamType.STRING, false, "JSON ChatAdministratorRights"),
                        p("for_channels", ParamDef.ParamType.BOOLEAN, false, "Untuk channel (bukan grup)"),
                    }},
                    {"getMyDefaultAdministratorRights", "Get Default Rights", "Dapatkan default hak admin", NodeType.ACTION, new ParamDef[]{
                        p("for_channels", ParamDef.ParamType.BOOLEAN, false, "Untuk channel"),
                    }},
                    {"setChatMenuButton", "Set Menu Button", "Atur tombol menu bot", NodeType.ACTION, new ParamDef[]{
                        p("menu_button", ParamDef.ParamType.STRING, false, "JSON MenuButton object"),
                    }},
                    {"getChatMenuButton", "Get Menu Button", "Dapatkan tombol menu bot", NodeType.ACTION, null},
                    {"setWebhook", "Set Webhook", "Atur webhook URL", NodeType.ACTION, new ParamDef[]{
                        p("url", ParamDef.ParamType.STRING, true, "URL webhook publik (HTTPS)"),
                        p("max_connections", ParamDef.ParamType.INTEGER, false, "Maks koneksi simultan (1-100)", "40"),
                        p("allowed_updates", ParamDef.ParamType.STRING, false, "JSON array tipe update, mis. [\"message\",\"callback_query\"]"),
                        p("secret_token", ParamDef.ParamType.STRING, false, "Token rahasia di header X-Telegram-Bot-Api-Secret-Token"),
                    }},
                    {"deleteWebhook", "Delete Webhook", "Hapus webhook (kembali ke getUpdates)", NodeType.ACTION, new ParamDef[]{
                        p("drop_pending_updates", ParamDef.ParamType.BOOLEAN, false, "Hapus update yang tertunda"),
                    }},
                    {"getWebhookInfo", "Get Webhook Info", "Info status webhook", NodeType.ACTION, null},
                    {"logOut", "Log Out", "Logout dari server Bot API", NodeType.ACTION, null},
                    {"close", "Close Bot", "Tutup instance bot", NodeType.ACTION, null},
                }));

        list.add(pack("Payments", "com.tgflowbot.ext.payments",
                "Send invoices, handle payments, manage star transactions and gifts.",
                "Commerce",
                new Object[][]{
                    {"sendInvoice", "Send Invoice", "Kirim invoice pembayaran", NodeType.ACTION, new ParamDef[]{
                        p("title", ParamDef.ParamType.STRING, true, "Judul produk (1-32 karakter)"),
                        p("description", ParamDef.ParamType.STRING, true, "Deskripsi (1-255 karakter)"),
                        p("payload", ParamDef.ParamType.STRING, true, "Payload bot-defined (1-128 byte)"),
                        p("provider_token", ParamDef.ParamType.STRING, true, "Token payment provider"),
                        p("currency", ParamDef.ParamType.STRING, true, "Mata uang 3 huruf, mis. USD"),
                        p("prices", ParamDef.ParamType.STRING, true, "JSON array LabeledPrice"),
                        p("max_tip_amount", ParamDef.ParamType.INTEGER, false, "Maks tip"),
                        p("suggested_tip_amounts", ParamDef.ParamType.STRING, false, "JSON array jumlah tip"),
                        p("start_parameter", ParamDef.ParamType.STRING, false, "Parameter deep-link"),
                        p("provider_data", ParamDef.ParamType.STRING, false, "JSON data untuk provider"),
                        p("photo_url", ParamDef.ParamType.STRING, false, "URL foto produk"),
                        p("photo_size", ParamDef.ParamType.INTEGER, false, "Ukuran foto"),
                        p("photo_width", ParamDef.ParamType.INTEGER, false, "Lebar foto"),
                        p("photo_height", ParamDef.ParamType.INTEGER, false, "Tinggi foto"),
                        p("need_name", ParamDef.ParamType.BOOLEAN, false, "Butuh nama pengirim"),
                        p("need_phone_number", ParamDef.ParamType.BOOLEAN, false, "Butuh nomor telepon"),
                        p("need_email", ParamDef.ParamType.BOOLEAN, false, "Butuh email"),
                        p("need_shipping_address", ParamDef.ParamType.BOOLEAN, false, "Butuh alamat pengiriman"),
                        p("send_phone_number_to_provider", ParamDef.ParamType.BOOLEAN, false, "Kirim telepon ke provider"),
                        p("send_email_to_provider", ParamDef.ParamType.BOOLEAN, false, "Kirim email ke provider"),
                        p("is_flexible", ParamDef.ParamType.BOOLEAN, false, "Harga fleksibel (butuh shipping)"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"createInvoiceLink", "Create Invoice Link", "Buat link invoice", NodeType.ACTION, new ParamDef[]{
                        p("title", ParamDef.ParamType.STRING, true, "Judul produk"),
                        p("description", ParamDef.ParamType.STRING, true, "Deskripsi"),
                        p("payload", ParamDef.ParamType.STRING, true, "Payload"),
                        p("provider_token", ParamDef.ParamType.STRING, true, "Token provider"),
                        p("currency", ParamDef.ParamType.STRING, true, "Mata uang"),
                        p("prices", ParamDef.ParamType.STRING, true, "JSON array LabeledPrice"),
                        p("max_tip_amount", ParamDef.ParamType.INTEGER, false, "Maks tip"),
                        p("suggested_tip_amounts", ParamDef.ParamType.STRING, false, "JSON array tip"),
                        p("provider_data", ParamDef.ParamType.STRING, false, "Data provider"),
                        p("photo_url", ParamDef.ParamType.STRING, false, "URL foto"),
                        p("photo_size", ParamDef.ParamType.INTEGER, false, "Ukuran foto"),
                        p("photo_width", ParamDef.ParamType.INTEGER, false, "Lebar foto"),
                        p("photo_height", ParamDef.ParamType.INTEGER, false, "Tinggi foto"),
                        p("need_name", ParamDef.ParamType.BOOLEAN, false, "Butuh nama"),
                        p("need_phone_number", ParamDef.ParamType.BOOLEAN, false, "Butuh telepon"),
                        p("need_email", ParamDef.ParamType.BOOLEAN, false, "Butuh email"),
                        p("need_shipping_address", ParamDef.ParamType.BOOLEAN, false, "Butuh alamat"),
                        p("send_phone_number_to_provider", ParamDef.ParamType.BOOLEAN, false, "Kirim telepon ke provider"),
                        p("send_email_to_provider", ParamDef.ParamType.BOOLEAN, false, "Kirim email ke provider"),
                        p("is_flexible", ParamDef.ParamType.BOOLEAN, false, "Harga fleksibel"),
                    }},
                    {"answerShippingQuery", "Answer Shipping", "Balas shipping query", NodeType.ACTION, new ParamDef[]{
                        p("shipping_query_id", ParamDef.ParamType.STRING, true, "ID shipping query"),
                        p("ok", ParamDef.ParamType.BOOLEAN, true, "Setujui pengiriman"),
                        p("shipping_options", ParamDef.ParamType.STRING, false, "JSON array ShippingOption"),
                        p("error_message", ParamDef.ParamType.STRING, false, "Pesan error (jika ok=false)"),
                    }},
                    {"answerPreCheckoutQuery", "Answer Pre-Checkout", "Balas pre-checkout query", NodeType.ACTION, new ParamDef[]{
                        p("pre_checkout_query_id", ParamDef.ParamType.STRING, true, "ID pre-checkout query"),
                        p("ok", ParamDef.ParamType.BOOLEAN, true, "Setujui pembayaran"),
                        p("error_message", ParamDef.ParamType.STRING, false, "Pesan error (jika ok=false)"),
                    }},
                    {"sendGift", "Send Gift", "Kirim hadiah ke user", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user penerima"),
                        p("gift_id", ParamDef.ParamType.STRING, true, "ID gift"),
                        p("pay_for_upgrade", ParamDef.ParamType.BOOLEAN, false, "Bayar untuk upgrade"),
                        p("text", ParamDef.ParamType.STRING, false, "Teks pesan"),
                        p("text_parse_mode", ParamDef.ParamType.STRING, false, "Format teks"),
                    }},
                    {"getAvailableGifts", "Get Available Gifts", "Daftar gift yang tersedia", NodeType.ACTION, null},
                    {"getStarTransactions", "Get Star Transactions", "Dapatkan transaksi star", NodeType.ACTION, new ParamDef[]{
                        p("offset", ParamDef.ParamType.INTEGER, false, "Offset"),
                        p("limit", ParamDef.ParamType.INTEGER, false, "Maks jumlah (1-100)", "100"),
                    }},
                    {"refundStarPayment", "Refund Star Payment", "Refund pembayaran star", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("telegram_payment_charge_id", ParamDef.ParamType.STRING, true, "ID charge Telegram"),
                    }},
                    {"editUserStarSubscription", "Edit Star Subscription", "Edit subscription star user", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("telegram_payment_charge_id", ParamDef.ParamType.STRING, true, "ID charge"),
                        p("is_canceled", ParamDef.ParamType.BOOLEAN, true, "Batalkan subscription"),
                    }},
                }));

        list.add(pack("Inline Mode & Games", "com.tgflowbot.ext.inline",
                "Answer inline queries, save messages, manage games and high scores.",
                "Communication",
                new Object[][]{
                    {"answerInlineQuery", "Answer Inline Query", "Balas inline query dari user", NodeType.ACTION, new ParamDef[]{
                        p("inline_query_id", ParamDef.ParamType.STRING, true, "ID inline query"),
                        p("results", ParamDef.ParamType.STRING, true, "JSON array InlineQueryResult"),
                        p("cache_time", ParamDef.ParamType.INTEGER, false, "Waktu cache (detik)", "300"),
                        p("is_personal", ParamDef.ParamType.BOOLEAN, false, "Hasil personal per user"),
                        p("next_offset", ParamDef.ParamType.STRING, false, "Offset untuk halaman berikutnya"),
                        p("button", ParamDef.ParamType.STRING, false, "JSON InlineQueryResultsButton"),
                    }},
                    {"savePreparedInlineMessage", "Save Prepared Message", "Simpan prepared inline message", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("result", ParamDef.ParamType.STRING, true, "JSON InlineQueryResult"),
                        p("allow_user_chats", ParamDef.ParamType.BOOLEAN, false, "Izinkan chat user"),
                        p("allow_bot_chats", ParamDef.ParamType.BOOLEAN, false, "Izinkan chat bot"),
                        p("allow_group_chats", ParamDef.ParamType.BOOLEAN, false, "Izinkan chat grup"),
                        p("allow_channel_chats", ParamDef.ParamType.BOOLEAN, false, "Izinkan channel"),
                    }},
                    {"sendGame", "Send Game", "Kirim game", NodeType.ACTION, new ParamDef[]{
                        p("game_short_name", ParamDef.ParamType.STRING, true, "Nama pendek game"),
                        p("disable_notification", ParamDef.ParamType.BOOLEAN, false, "Kirim tanpa notifikasi"),
                        p("protect_content", ParamDef.ParamType.BOOLEAN, false, "Cegah forward/simpan konten"),
                        p("reply_to_message_id", ParamDef.ParamType.INTEGER, false, "ID pesan yang dibalas"),
                        p("reply_markup", ParamDef.ParamType.STRING, false, "JSON inline keyboard"),
                    }},
                    {"setGameScore", "Set Game Score", "Atur skor game user", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("score", ParamDef.ParamType.INTEGER, true, "Skor baru"),
                        p("force", ParamDef.ParamType.BOOLEAN, false, "Paksa update skor"),
                        p("disable_edit_message", ParamDef.ParamType.BOOLEAN, false, "Nonaktifkan edit pesan"),
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan game"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                    }},
                    {"getGameHighScores", "Get Game Scores", "Skor tertinggi game", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                        p("message_id", ParamDef.ParamType.INTEGER, false, "ID pesan game"),
                        p("inline_message_id", ParamDef.ParamType.STRING, false, "ID inline message"),
                    }},
                }));

        list.add(pack("Verification & Privacy", "com.tgflowbot.ext.verify",
                "Verify users, chats and manage verification status.",
                "Moderation",
                new Object[][]{
                    {"verifyUser", "Verify User", "Verifikasi user", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user yang diverifikasi"),
                        p("custom_description", ParamDef.ParamType.STRING, false, "Deskripsi kustom"),
                    }},
                    {"verifyChat", "Verify Chat", "Verifikasi chat", NodeType.ACTION, new ParamDef[]{
                        p("chat_id", ParamDef.ParamType.STRING, true, "ID chat yang diverifikasi"),
                        p("custom_description", ParamDef.ParamType.STRING, false, "Deskripsi kustom"),
                    }},
                    {"removeUserVerification", "Remove User Verify", "Hapus verifikasi user", NodeType.ACTION, new ParamDef[]{
                        p("user_id", ParamDef.ParamType.INTEGER, true, "ID user"),
                    }},
                    {"removeChatVerification", "Remove Chat Verify", "Hapus verifikasi chat", NodeType.ACTION, new ParamDef[]{
                        p("chat_id", ParamDef.ParamType.STRING, true, "ID chat"),
                    }},
                }));

        list.add(pack("Phone Controls", "com.tgflowbot.ext.phone",
                "Control phone hardware: flashlight, vibrate, battery info, volume, brightness, clipboard, TTS, STT, and more.",
                "Device",
                new Object[][]{
                    {"_phone_flashlight", "Flashlight", "Nyalakan/matikan senter", NodeType.ACTION, new ParamDef[]{
                        p("state", ParamDef.ParamType.STRING, true, "on atau off", "on"),
                    }},
                    {"_phone_vibrate", "Vibrate", "Getar ponsel", NodeType.ACTION, new ParamDef[]{
                        p("duration", ParamDef.ParamType.INTEGER, false, "Durasi getar (ms)", "500"),
                    }},
                    {"_phone_toast", "Toast", "Tampilkan pesan di layar", NodeType.ACTION, new ParamDef[]{
                        p("message", ParamDef.ParamType.STRING, true, "Pesan yang ditampilkan"),
                    }},
                    {"_phone_battery", "Battery", "Dapatkan info baterai", NodeType.ACTION, null},
                    {"_phone_device_info", "Device Info", "Info perangkat", NodeType.ACTION, null},
                    {"_phone_open_url", "Open URL", "Buka URL di browser", NodeType.ACTION, new ParamDef[]{
                        p("url", ParamDef.ParamType.STRING, true, "URL yang dibuka"),
                    }},
                    {"_phone_clipboard_set", "Clipboard Set", "Salin teks ke clipboard", NodeType.ACTION, new ParamDef[]{
                        p("text", ParamDef.ParamType.STRING, true, "Teks yang disalin"),
                    }},
                    {"_phone_volume", "Volume", "Atur volume media 0-100", NodeType.ACTION, new ParamDef[]{
                        p("level", ParamDef.ParamType.INTEGER, true, "Level volume (0-100)", "50"),
                    }},
                    {"_phone_brightness", "Brightness", "Atur kecerahan layar 0-255", NodeType.ACTION, new ParamDef[]{
                        p("level", ParamDef.ParamType.INTEGER, true, "Level kecerahan (0-255)", "128"),
                    }},
                    {"_phone_tts", "Text to Speech", "Ucapkan teks dengan TTS", NodeType.ACTION, new ParamDef[]{
                        p("text", ParamDef.ParamType.STRING, false, "Teks diucapkan (kosongkan = pesan masuk)"),
                        p("language", ParamDef.ParamType.STRING, false, "Kode bahasa (mis. id-ID)", "id-ID"),
                    }},
                    {"_phone_stt", "Speech to Text", "Dengar suara dan ubah ke teks", NodeType.ACTION, new ParamDef[]{
                        p("prompt", ParamDef.ParamType.STRING, false, "Teks prompt saat mendengarkan", "Silakan bicara"),
                        p("timeout_sec", ParamDef.ParamType.INTEGER, false, "Batas waktu (detik)", "5"),
                    }},
                }));

        list.add(pack("File Operations", "com.tgflowbot.ext.file",
                "Read, write, append, delete, and list files on the device storage.",
                "Data",
                new Object[][]{
                    {"_file_read", "File Read", "Baca isi file", NodeType.ACTION, new ParamDef[]{
                        p("path", ParamDef.ParamType.STRING, true, "Path file yang dibaca"),
                    }},
                    {"_file_write", "File Write", "Tulis teks ke file", NodeType.ACTION, new ParamDef[]{
                        p("path", ParamDef.ParamType.STRING, true, "Path file"),
                        p("content", ParamDef.ParamType.STRING, true, "Isi yang ditulis"),
                    }},
                    {"_file_append", "File Append", "Tambah teks ke akhir file", NodeType.ACTION, new ParamDef[]{
                        p("path", ParamDef.ParamType.STRING, true, "Path file"),
                        p("content", ParamDef.ParamType.STRING, true, "Teks yang ditambahkan"),
                    }},
                    {"_file_delete", "File Delete", "Hapus file", NodeType.ACTION, new ParamDef[]{
                        p("path", ParamDef.ParamType.STRING, true, "Path file yang dihapus"),
                    }},
                    {"_file_exists", "File Exists", "Cek apakah file ada", NodeType.ACTION, new ParamDef[]{
                        p("path", ParamDef.ParamType.STRING, true, "Path file yang dicek"),
                    }},
                    {"_file_list", "File List", "Daftar file dalam direktori", NodeType.ACTION, new ParamDef[]{
                        p("dir", ParamDef.ParamType.STRING, true, "Path direktori"),
                    }},
                }));

        list.add(pack("Math & Logic", "com.tgflowbot.ext.math",
                "Mathematical operations: add, subtract, multiply, divide, power, sqrt, round, random, min, max, clamp, and more.",
                "Utility",
                new Object[][]{
                    {"_add", "Add", "Penjumlahan a + b", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_subtract", "Subtract", "Pengurangan a - b", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_multiply", "Multiply", "Perkalian a * b", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_divide", "Divide", "Pembagian a / b", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_modulo", "Modulo", "Sisa bagi a % b", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_power", "Power", "a pangkat b", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Basis"), p("b", ParamDef.ParamType.FLOAT, true, "Pangkat"),
                    }},
                    {"_sqrt", "Square Root", "Akar kuadrat", NodeType.ACTION, new ParamDef[]{
                        p("value", ParamDef.ParamType.FLOAT, true, "Nilai"),
                    }},
                    {"_abs", "Absolute", "Nilai absolut", NodeType.ACTION, new ParamDef[]{
                        p("value", ParamDef.ParamType.FLOAT, true, "Nilai"),
                    }},
                    {"_round", "Round", "Pembulatan ke integer terdekat", NodeType.ACTION, new ParamDef[]{
                        p("value", ParamDef.ParamType.FLOAT, true, "Nilai"),
                    }},
                    {"_floor", "Floor", "Pembulatan ke bawah", NodeType.ACTION, new ParamDef[]{
                        p("value", ParamDef.ParamType.FLOAT, true, "Nilai"),
                    }},
                    {"_ceil", "Ceil", "Pembulatan ke atas", NodeType.ACTION, new ParamDef[]{
                        p("value", ParamDef.ParamType.FLOAT, true, "Nilai"),
                    }},
                    {"_min", "Min", "Nilai terkecil dari dua angka", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_max", "Max", "Nilai terbesar dari dua angka", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.FLOAT, true, "Nilai A"), p("b", ParamDef.ParamType.FLOAT, true, "Nilai B"),
                    }},
                    {"_clamp", "Clamp", "Batasi nilai antara min dan max", NodeType.ACTION, new ParamDef[]{
                        p("value", ParamDef.ParamType.FLOAT, true, "Nilai"),
                        p("min", ParamDef.ParamType.FLOAT, true, "Batas bawah"),
                        p("max", ParamDef.ParamType.FLOAT, true, "Batas atas"),
                    }},
                    {"_random", "Random Number", "Angka acak antara min-max", NodeType.ACTION, new ParamDef[]{
                        p("min", ParamDef.ParamType.FLOAT, false, "Batas bawah", "0"),
                        p("max", ParamDef.ParamType.FLOAT, false, "Batas atas", "100"),
                    }},
                }));

        list.add(pack("Variables & Lists", "com.tgflowbot.ext.varlist",
                "Store, retrieve, and manipulate variables and lists throughout your workflow.",
                "Data",
                new Object[][]{
                    {"_set_variable", "Set Variable", "Simpan nilai ke variabel", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("value", ParamDef.ParamType.STRING, false, "Nilai yang disimpan"),
                    }},
                    {"_get_variable", "Get Variable", "Ambil nilai dari variabel", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("default", ParamDef.ParamType.STRING, false, "Nilai default jika tidak ditemukan"),
                    }},
                    {"_var_add", "Var Add", "Tambah angka ke variable", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("value", ParamDef.ParamType.FLOAT, true, "Angka penambah"),
                    }},
                    {"_var_subtract", "Var Subtract", "Kurang angka dari variable", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("value", ParamDef.ParamType.FLOAT, true, "Angka pengurang"),
                    }},
                    {"_var_multiply", "Var Multiply", "Kali variable dengan angka", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("value", ParamDef.ParamType.FLOAT, true, "Angka pengali"),
                    }},
                    {"_var_divide", "Var Divide", "Bagi variable dengan angka", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("value", ParamDef.ParamType.FLOAT, true, "Angka pembagi"),
                    }},
                    {"_var_append", "Var Append", "Gabung teks ke variable", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                        p("value", ParamDef.ParamType.STRING, true, "Teks yang ditambahkan"),
                    }},
                    {"_var_clear", "Var Clear", "Hapus isi variable", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                    }},
                    {"_var_delete", "Var Delete", "Hapus variable", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama variabel"),
                    }},
                    {"_list_create", "List Create", "Buat list dari teks", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                        p("items", ParamDef.ParamType.STRING, false, "Item, pisahkan dengan koma"),
                    }},
                    {"_list_add", "List Add", "Tambah item ke list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                        p("item", ParamDef.ParamType.STRING, true, "Item yang ditambahkan"),
                    }},
                    {"_list_remove", "List Remove", "Hapus item dari list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                        p("index", ParamDef.ParamType.INTEGER, true, "Index item yang dihapus"),
                    }},
                    {"_list_get", "List Get", "Ambil item dari list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                        p("index", ParamDef.ParamType.INTEGER, true, "Index item yang diambil"),
                    }},
                    {"_list_size", "List Size", "Jumlah item dalam list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                    }},
                    {"_list_clear", "List Clear", "Kosongkan list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                    }},
                    {"_list_join", "List Join", "Gabung item list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                        p("separator", ParamDef.ParamType.STRING, false, "Pemisah antar item", ", "),
                    }},
                    {"_list_shuffle", "List Shuffle", "Acak urutan list", NodeType.ACTION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama list"),
                    }},
                }));

        list.add(pack("AI Chat", "com.tgflowbot.ext.ai",
                "Chat with AI providers (OpenAI, Claude, Gemini, Groq, LLamaCPP) and use AI phone tools.",
                "Intelligence",
                new Object[][]{
                    {"ai_chat", "AI Chat", "Chat dengan AI", NodeType.ACTION, new ParamDef[]{
                        p("prompt_template", ParamDef.ParamType.STRING, false, "Template prompt", "{{text}}"),
                        p("provider", ParamDef.ParamType.STRING, false, "openai/claude/gemini/groq/llamacpp", "openai"),
                        p("model", ParamDef.ParamType.STRING, false, "Nama model (opsional)"),
                        p("system_prompt", ParamDef.ParamType.STRING, false, "System prompt (opsional)"),
                        p("custom_endpoint", ParamDef.ParamType.STRING, false, "Endpoint custom (opsional)"),
                        p("temperature", ParamDef.ParamType.FLOAT, false, "Kreativitas 0-1", "0.7"),
                        p("max_tokens", ParamDef.ParamType.INTEGER, false, "Maksimal token respon", "1024"),
                        p("use_phone_tools", ParamDef.ParamType.BOOLEAN, false, "Aktifkan phone tools AI", "false"),
                    }},
                }));

        list.add(pack("HTTP & Web", "com.tgflowbot.ext.http",
                "Make HTTP requests to REST APIs and integrate with web services.",
                "Integration",
                new Object[][]{
                    {"_http_request", "HTTP Request", "Kirim request HTTP", NodeType.ACTION, new ParamDef[]{
                        p("method", ParamDef.ParamType.STRING, false, "GET/POST/PUT/DELETE", "GET"),
                        p("url", ParamDef.ParamType.STRING, true, "URL tujuan"),
                        p("headers", ParamDef.ParamType.STRING, false, "Header dalam format JSON"),
                        p("body", ParamDef.ParamType.STRING, false, "Body request (untuk POST/PUT)"),
                    }},
                }));

        list.add(pack("Text Processing", "com.tgflowbot.ext.text",
                "String manipulation: append, replace, and transform text in your workflow.",
                "Utility",
                new Object[][]{
                    {"_text_append", "Text Append", "Gabung dua teks", NodeType.ACTION, new ParamDef[]{
                        p("a", ParamDef.ParamType.STRING, true, "Teks pertama"),
                        p("b", ParamDef.ParamType.STRING, true, "Teks kedua"),
                    }},
                    {"_text_replace", "Text Replace", "Cari dan ganti teks", NodeType.ACTION, new ParamDef[]{
                        p("text", ParamDef.ParamType.STRING, true, "Teks asal"),
                        p("search", ParamDef.ParamType.STRING, true, "Kata/frasa yang dicari"),
                        p("replace", ParamDef.ParamType.STRING, false, "Kata/frasa pengganti"),
                    }},
                }));

        list.add(pack("Flow Control", "com.tgflowbot.ext.flow",
                "Control workflow execution: delay, repeat, loop break, wait until, return, and log messages.",
                "Utility",
                new Object[][]{
                    {"_delay", "Delay", "Tunggu sebelum lanjut (ms)", NodeType.ACTION, new ParamDef[]{
                        p("ms", ParamDef.ParamType.INTEGER, true, "Durasi tunda (ms)", "1000"),
                    }},
                    {"_repeat", "Repeat", "Ulangi aliran N kali", NodeType.ACTION, new ParamDef[]{
                        p("count", ParamDef.ParamType.INTEGER, true, "Jumlah pengulangan", "3"),
                    }},
                    {"_wait_until", "Wait Until", "Tunggu sampai kondisi terpenuhi", NodeType.ACTION, new ParamDef[]{
                        p("condition_var", ParamDef.ParamType.STRING, true, "Nama variabel yang dicek"),
                        p("expected", ParamDef.ParamType.STRING, false, "Nilai yang diharapkan"),
                        p("timeout_ms", ParamDef.ParamType.INTEGER, false, "Batas waktu (ms)", "5000"),
                        p("interval_ms", ParamDef.ParamType.INTEGER, false, "Interval cek (ms)", "200"),
                    }},
                    {"_loop_break", "Loop Break", "Hentikan perulangan", NodeType.ACTION, null},
                    {"_return", "Return", "Hentikan flow", NodeType.ACTION, null},
                    {"_log", "Log Message", "Tulis pesan ke log", NodeType.ACTION, new ParamDef[]{
                        p("message", ParamDef.ParamType.STRING, false, "Pesan log (kosongkan = pesan masuk)"),
                    }},
                }));

        list.add(pack("Triggers", "com.tgflowbot.ext.triggers",
                "Workflow triggers: incoming messages, commands, callback queries, inline queries, chat member updates, scheduled cron, intervals, HTTP polling, webhooks, voice commands, and manual triggers.",
                "Flow",
                new Object[][]{
                    {"_on_message", "On Message", "Trigger saat ada pesan teks baru", NodeType.TRIGGER, new ParamDef[]{
                        p("command", ParamDef.ParamType.STRING, false, "Filter perintah (mis. /start)"),
                        p("filter", ParamDef.ParamType.STRING, false, "Filter kata dalam pesan"),
                    }},
                    {"_on_photo", "On Photo", "Trigger saat ada foto masuk", NodeType.TRIGGER, null},
                    {"_on_video", "On Video", "Trigger saat ada video masuk", NodeType.TRIGGER, null},
                    {"_on_document", "On Document", "Trigger saat ada file/dokumen masuk", NodeType.TRIGGER, null},
                    {"_on_audio", "On Audio", "Trigger saat ada audio masuk", NodeType.TRIGGER, null},
                    {"_on_voice", "On Voice", "Trigger saat ada voice message masuk", NodeType.TRIGGER, null},
                    {"_on_animation", "On Animation", "Trigger saat ada animasi/GIF masuk", NodeType.TRIGGER, null},
                    {"_on_sticker", "On Sticker", "Trigger saat ada sticker masuk", NodeType.TRIGGER, null},
                    {"_on_location", "On Location", "Trigger saat ada lokasi masuk", NodeType.TRIGGER, null},
                    {"_on_contact", "On Contact", "Trigger saat ada kontak masuk", NodeType.TRIGGER, null},
                    {"_on_poll", "On Poll", "Trigger saat ada poll masuk", NodeType.TRIGGER, null},
                    {"_on_edited_message", "On Edited Message", "Trigger saat pesan diedit", NodeType.TRIGGER, null},
                    {"_on_channel_post", "On Channel Post", "Trigger saat ada posting di channel", NodeType.TRIGGER, null},
                    {"_on_callback_query", "On Callback Query", "Trigger saat tombol inline ditekan", NodeType.TRIGGER, new ParamDef[]{
                        p("data", ParamDef.ParamType.STRING, false, "Filter data callback (kosongkan = semua)"),
                    }},
                    {"_on_inline_query", "On Inline Query", "Trigger saat @bot digunakan di chat", NodeType.TRIGGER, new ParamDef[]{
                        p("query", ParamDef.ParamType.STRING, false, "Filter teks query (kosongkan = semua)"),
                    }},
                    {"_on_chosen_inline_result", "On Chosen Inline", "Trigger saat user memilih hasil inline", NodeType.TRIGGER, null},
                    {"_on_chat_member", "On Chat Member", "Trigger saat anggota bergabung/keluar", NodeType.TRIGGER, new ParamDef[]{
                        p("status", ParamDef.ParamType.STRING, false, "Filter status: member/administrator/left/kicked"),
                    }},
                    {"_on_my_chat_member", "On Bot Chat Member", "Trigger saat bot ditambah/dihapus dari grup", NodeType.TRIGGER, null},
                    {"_on_chat_join_request", "On Join Request", "Trigger saat ada request bergabung", NodeType.TRIGGER, null},
                    {"_on_poll_answer", "On Poll Answer", "Trigger saat user memilih jawaban poll", NodeType.TRIGGER, null},
                    {"_on_pre_checkout_query", "On Pre Checkout", "Trigger sebelum pembayaran diproses", NodeType.TRIGGER, null},
                    {"_on_shipping_query", "On Shipping Query", "Trigger saat user memilih shipping", NodeType.TRIGGER, null},
                    {"_on_webhook_telegram", "On Webhook (Telegram)", "Trigger saat ada update dari webhook Telegram", NodeType.TRIGGER, new ParamDef[]{
                        p("url", ParamDef.ParamType.STRING, false, "URL webhook publik (kosongkan = pakai yg sudah diset)"),
                    }},
                    {"_on_listening", "On Listening", "Trigger saat mendengar suara (STT)", NodeType.TRIGGER, new ParamDef[]{
                        p("prompt", ParamDef.ParamType.STRING, false, "Teks prompt", "Silakan bicara"),
                        p("timeout_sec", ParamDef.ParamType.INTEGER, false, "Batas waktu (detik)", "10"),
                    }},
                    {"_on_schedule", "On Schedule (Cron)", "Trigger berdasarkan jadwal cron", NodeType.TRIGGER, new ParamDef[]{
                        p("cron", ParamDef.ParamType.STRING, true, "Ekspresi cron, mis. */5 * * * *"),
                    }},
                    {"_on_interval", "On Interval", "Trigger setiap interval detik", NodeType.TRIGGER, new ParamDef[]{
                        p("interval_sec", ParamDef.ParamType.INTEGER, true, "Interval (detik)", "60"),
                    }},
                    {"_on_http_poll", "On HTTP Poll", "Poll HTTP endpoint berkala", NodeType.TRIGGER, new ParamDef[]{
                        p("url", ParamDef.ParamType.STRING, true, "Endpoint yang di-poll"),
                        p("interval_sec", ParamDef.ParamType.INTEGER, false, "Interval (detik)", "60"),
                    }},
                    {"_on_webhook", "On Webhook", "Trigger via HTTP lokal (port 8080)", NodeType.TRIGGER, new ParamDef[]{
                        p("path", ParamDef.ParamType.STRING, false, "Path webhook", "/webhook"),
                        p("secret_token", ParamDef.ParamType.STRING, false, "Token rahasia (opsional)"),
                    }},
                    {"_on_manual", "Manual Trigger", "Trigger manual dari UI", NodeType.TRIGGER, new ParamDef[]{
                        p("input_data", ParamDef.ParamType.STRING, false, "Data JSON input manual", "{}"),
                    }},
                }));

        list.add(pack("Conditions & Outputs", "com.tgflowbot.ext.conditions",
                "Flow conditions (text matching, media, chat type, admin, number comparison) and output nodes (reply, forward, delete, kick, log).",
                "Flow",
                new Object[][]{
                    {"contains", "Contains", "Cek apakah teks mengandung kata", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Kata/frasa yang dicari"),
                        p("case_sensitive", ParamDef.ParamType.BOOLEAN, false, "Peka huruf besar/kecil", "false"),
                    }},
                    {"equals", "Equals", "Cek apakah teks sama persis", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Nilai pembanding"),
                        p("case_sensitive", ParamDef.ParamType.BOOLEAN, false, "Peka huruf besar/kecil", "false"),
                    }},
                    {"startsWith", "Starts With", "Cek awalan teks", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Awalan yang dicek"),
                        p("case_sensitive", ParamDef.ParamType.BOOLEAN, false, "Peka huruf besar/kecil", "false"),
                    }},
                    {"matches", "Regex Match", "Cocokkan regex", NodeType.CONDITION, new ParamDef[]{
                        p("pattern", ParamDef.ParamType.STRING, true, "Pola regex"),
                        p("case_sensitive", ParamDef.ParamType.BOOLEAN, false, "Peka huruf besar/kecil", "false"),
                    }},
                    {"hasMedia", "Has Media", "Cek apakah ada media (foto/video/doc/audio)", NodeType.CONDITION, new ParamDef[]{
                        p("media_type", ParamDef.ParamType.STRING, false, "photo/video/document/audio/voice/animation/sticker/location/contact/poll (kosongkan = media apapun)"),
                    }},
                    {"hasPhoto", "Has Photo", "Cek apakah ada foto", NodeType.CONDITION, null},
                    {"hasVideo", "Has Video", "Cek apakah ada video", NodeType.CONDITION, null},
                    {"hasDocument", "Has Document", "Cek apakah ada file/dokumen", NodeType.CONDITION, null},
                    {"hasAudio", "Has Audio", "Cek apakah ada audio", NodeType.CONDITION, null},
                    {"hasVoice", "Has Voice", "Cek apakah ada voice message", NodeType.CONDITION, null},
                    {"hasAnimation", "Has Animation", "Cek apakah ada animasi/GIF", NodeType.CONDITION, null},
                    {"hasSticker", "Has Sticker", "Cek apakah ada sticker", NodeType.CONDITION, null},
                    {"hasLocation", "Has Location", "Cek apakah ada lokasi", NodeType.CONDITION, null},
                    {"hasContact", "Has Contact", "Cek apakah ada kontak", NodeType.CONDITION, null},
                    {"hasPoll", "Has Poll", "Cek apakah ada poll", NodeType.CONDITION, null},
                    {"hasDice", "Has Dice", "Cek apakah ada dadu/emoji acak", NodeType.CONDITION, null},
                    {"isForwarded", "Is Forwarded", "Cek apakah pesan diteruskan", NodeType.CONDITION, null},
                    {"isReply", "Is Reply", "Cek apakah pesan balasan", NodeType.CONDITION, null},
                    {"isBot", "Is Bot", "Cek apakah pengirim adalah bot", NodeType.CONDITION, null},
                    {"isCommand", "Is Command", "Cek apakah pesan dimulai dengan /", NodeType.CONDITION, null},
                    {"chatType", "Chat Type", "Cek tipe chat", NodeType.CONDITION, new ParamDef[]{
                        p("type", ParamDef.ParamType.STRING, true, "private/group/supergroup/channel"),
                    }},
                    {"isAdmin", "Is Admin", "Cek apakah pengirim admin grup", NodeType.CONDITION, null},
                    {"alwaysTrue", "Always True", "Selalu true", NodeType.CONDITION, null},
                    {"alwaysFalse", "Always False", "Selalu false", NodeType.CONDITION, null},
                    {"_compare", "Compare", "Bandingkan dua angka", NodeType.CONDITION, new ParamDef[]{
                        p("a", ParamDef.ParamType.STRING, true, "Nilai A"),
                        p("b", ParamDef.ParamType.STRING, true, "Nilai B"),
                        p("operator", ParamDef.ParamType.STRING, false, "==, !=, >, <, >=, <=", "=="),
                    }},
                    {"endsWith", "Ends With", "Cek apakah teks diakhiri kata tertentu", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Akhiran yang dicek"),
                        p("case_sensitive", ParamDef.ParamType.BOOLEAN, false, "Peka huruf besar/kecil", "false"),
                    }},
                    {"isEmpty", "Is Empty", "Cek apakah teks kosong/null", NodeType.CONDITION, null},
                    {"isNumeric", "Is Numeric", "Cek apakah teks berupa angka", NodeType.CONDITION, null},
                    {"length", "Length", "Bandingkan panjang teks", NodeType.CONDITION, new ParamDef[]{
                        p("operator", ParamDef.ParamType.STRING, false, "==, !=, >, <, >=, <=", "=="),
                        p("value", ParamDef.ParamType.INTEGER, true, "Panjang yang dibandingkan"),
                    }},
                    {"isBetween", "Is Between", "Cek apakah nilai di antara dua angka", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Nilai yang dicek (bisa pakai {{placeholder}})"),
                        p("min", ParamDef.ParamType.STRING, true, "Nilai minimum"),
                        p("max", ParamDef.ParamType.STRING, true, "Nilai maksimum"),
                    }},
                    {"_in", "In List", "Cek apakah nilai ada dalam daftar", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Nilai yang dicek"),
                        p("list", ParamDef.ParamType.STRING, true, "Daftar pisah koma (mis: a,b,c)"),
                        p("case_sensitive", ParamDef.ParamType.BOOLEAN, false, "Peka huruf besar/kecil", "false"),
                    }},
                    {"notEmpty", "Not Empty", "Cek apakah nilai tidak kosong", NodeType.CONDITION, new ParamDef[]{
                        p("value", ParamDef.ParamType.STRING, true, "Nilai yang dicek (bisa pakai {{placeholder}})"),
                    }},
                    {"hasKey", "Has Key", "Cek apakah key tertentu ada di data pesan", NodeType.CONDITION, new ParamDef[]{
                        p("key", ParamDef.ParamType.STRING, true, "Nama key (mis: photo, from.id)"),
                    }},
                    {"reply", "Reply", "Balas pesan", NodeType.OUTPUT, new ParamDef[]{
                        p("text", ParamDef.ParamType.STRING, true, "Isi balasan"),
                        p("parse_mode", ParamDef.ParamType.STRING, false, "Format teks"),
                    }},
                    {"forward", "Forward", "Forward ke chat lain", NodeType.OUTPUT, new ParamDef[]{
                        p("target_chat_id", ParamDef.ParamType.STRING, true, "Chat ID tujuan forward"),
                    }},
                    {"_output_delete", "Delete Message", "Hapus pesan incoming", NodeType.OUTPUT, null},
                    {"_output_pin", "Pin Message", "Sematkan pesan incoming", NodeType.OUTPUT, null},
                    {"_output_kick", "Kick Member", "Tendang pengirim dari grup", NodeType.OUTPUT, new ParamDef[]{
                        p("until_date", ParamDef.ParamType.INTEGER, false, "Unix timestamp sampai kapan (0 = permanen)"),
                    }},
                    {"log", "Log", "Catat ke log lokal", NodeType.OUTPUT, new ParamDef[]{
                        p("message", ParamDef.ParamType.STRING, false, "Pesan log (kosongkan = pesan masuk)"),
                    }},
                }));

        return list;
    }
}
