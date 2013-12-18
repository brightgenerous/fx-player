package com.brightgenerous.fxplayer.application.playlist;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;

class ImageSaveUtils {

    public static enum Type {

        PNG("png", ".png"), JPG("jpg", ".jpg"), GIF("gif", ".gif");

        private final String formatName;

        private final String extension;

        private Type(String formatName, String extension) {
            this.formatName = formatName;
            this.extension = extension;
        }

        public String getFormatName() {
            return formatName;
        }

        public String getExtension() {
            return extension;
        }

        private Boolean useful;

        public boolean useful() {
            check: if (useful == null) {
                String[] formats = ImageIO.getWriterFormatNames();
                if ((formats != null) && (0 < formats.length)) {
                    for (String format : formats) {
                        if (format.equals(formatName)) {
                            useful = Boolean.TRUE;
                            break check;
                        }
                    }
                }
                useful = Boolean.FALSE;
            }
            return useful.booleanValue();
        }
    }

    private ImageSaveUtils() {
    }

    public static String getFileDescription() {
        StringBuilder sb = new StringBuilder();
        for (Type type : Type.values()) {
            if (type.useful()) {
                if (0 < sb.length()) {
                    sb.append("/");
                }
                sb.append(type.formatName);
            }
        }
        sb.insert(0, "Image(");
        sb.append(")");
        return sb.toString();
    }

    public static String[] getExtensions() {
        List<String> ret = new ArrayList<>();
        for (Type type : Type.values()) {
            if (type.useful()) {
                ret.add("*" + type.extension);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    private static Type guess(File file) {
        if (file == null) {
            return null;
        }
        Type ret = Type.PNG;
        {
            String name = file.getName().toLowerCase();
            for (Type type : Type.values()) {
                if (name.endsWith(type.extension)) {
                    ret = type;
                    break;
                }
            }
        }
        if (!ret.useful()) {
            ret = Type.PNG;
        }
        return ret;
    }

    public static String escapeFileName(String fileName) {
        return fileName.replaceAll("[\\s\\\\/:*?\"<>\\|]{1}", "_");
    }

    public static File save(File file, Image image) {

        Type type = guess(file);
        File out;
        out: {
            String base = file.getAbsolutePath();
            if (base.toLowerCase().endsWith(type.getExtension())) {
                // here
                // when over write, must be confirmed.
                //   or create the file.
                out = file;
                break out;
            }
            String name;
            int index = 0;
            do {
                if (index == 0) {
                    name = base;
                } else {
                    name = base + "_" + index;
                }
                index++;
                out = new File(name + type.getExtension());
            } while (out.exists());
        }

        boolean ret = false;

        try {
            BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
            if (type.equals(Type.JPG)) {
                bi = convertToRGB(bi);
            }
            ImageIO.write(bi, type.getFormatName(), out);
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!ret) {
            return null;
        }
        return out;
    }

    private static BufferedImage convertToRGB(BufferedImage image) {
        switch (image.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
                return image;
        }
        BufferedImage ret = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        ret.createGraphics().drawImage(image, 0, 0, null);
        return ret;
    }
}
