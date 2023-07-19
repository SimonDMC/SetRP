package com.simondmc.setrp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;

public final class SetRP extends JavaPlugin implements Listener {

    public static SetRP plugin;
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getServer().getPluginManager().registerEvents(this, this);
        plugin = this;
        plugin.saveResource("template_rp.zip", false);

        // start web server
        try {
            WebServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        WebServer.stop();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!label.equalsIgnoreCase("setrp")) return true;
        Player p = (Player) sender;
        p.sendMessage("[INFO] Task begin");

        // clone resource pack
        try {
            Zip.unzip(plugin.getDataFolder() + "/template_rp.zip", plugin.getDataFolder() + "/rp_build");
        } catch (IOException e) {
            p.sendMessage("[ERROR] Couldn't clone template resource pack");
            throw new RuntimeException(e);
        }
        p.sendMessage("[INFO] Cloned template resource pack");

        // download file
        try {
            downloadFile(args[0]);
        } catch (IOException e) {
            p.sendMessage("[ERROR] Couldn't download file");
            throw new RuntimeException(e);
        }
        p.sendMessage("[INFO] Downloaded file");

        // zip resource pack
        try {
            List<File> files = List.of(new File(plugin.getDataFolder() + "/rp_build/assets"), new File(plugin.getDataFolder() + "/rp_build/pack.mcmeta"));
            Zip.zip(files, plugin.getDataFolder() + "/rp.zip");
        } catch (IOException e) {
            p.sendMessage("[ERROR] Couldn't zip resource pack");
            throw new RuntimeException(e);
        }
        p.sendMessage("[INFO] Zipped resource pack");

        // get sha1
        byte[] sha1;
        try {
            sha1 = createSha1(new File(plugin.getDataFolder() + "/rp.zip"));
        } catch (Exception e) {
            p.sendMessage("[ERROR] Couldn't get sha1");
            throw new RuntimeException(e);
        }
        p.sendMessage("[INFO] Got sha1");

        // send rp to players
        String ip = Bukkit.getServer().getIp();
        if (ip.equals("")) {
            ip = "localhost";
        }
        // append sha1 to url due to MC-164316
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setResourcePack("http://" + ip + ":26668#" + sha1, sha1);
        }
        p.sendMessage("[INFO] Sent resource pack to players");

        return true;
    }

    @EventHandler
    public void rpStatus(PlayerResourcePackStatusEvent e) {
        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
            e.getPlayer().kickPlayer("You must accept the resource pack to play!");
        }
    }

    // download file as audio.ogg into resources/downloads
    public void downloadFile(String url) throws IOException {
        File downloadedFile = new File(plugin.getDataFolder() + "/rp_build/assets/minecraft/sounds/custom/audio.ogg");

        // if file exists, delete it
        if (downloadedFile.exists()) {
            downloadedFile.delete();
        }

        // download file
        ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
        downloadedFile.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(downloadedFile);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

    public byte[] createSha1(File file) throws Exception  {
        FileInputStream fileInputStream = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
        byte[] bytes = new byte[1024];
        // read all file content
        while (digestInputStream.read(bytes) > 0);
        return digest.digest();
    }
}
