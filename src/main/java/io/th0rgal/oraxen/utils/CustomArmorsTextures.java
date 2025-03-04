package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Color;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomArmorsTextures {

    private final Map<Integer, String> usedColors = new HashMap<>();
    private final List<BufferedImage> layers1 = new ArrayList<>();
    private final List<BufferedImage> layers2 = new ArrayList<>();
    private BufferedImage layer1;
    private int layer1Width = 0;
    private int layer1Height = 0;
    private BufferedImage layer2;
    private int layer2Width = 0;
    private int layer2Height = 0;

    public boolean registerImage(File file) throws IOException {

        String name = file.getName();

        if (!name.endsWith(".png"))
            return false;

        if (name.equals("leather_layer_1.png")) {
            layer1 = ImageIO.read(file);
            layer1Width += layer1.getWidth();
            if (layer1.getHeight() > layer1Height)
                layer1Height = layer1.getHeight();
            return true;
        }

        if (name.equals("leather_layer_2.png")) {
            layer2 = ImageIO.read(file);
            layer2Width += layer2.getWidth();
            if (layer2.getHeight() > layer2Height)
                layer2Height = layer2.getHeight();
            return true;
        }

        return name.contains("armor_layer_") && handleArmorLayer(name, file);
    }

    private boolean handleArmorLayer(String name, File file) throws IOException {
        if (name.endsWith("_e.png"))
            return true;

        String prefix = name.split("armor_layer_")[0];
        ItemBuilder builder = null;
        for (String suffix : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            builder = OraxenItems.getItemById(prefix + suffix);
            if (builder != null)
                break;
        }
        if (builder == null) {
            Message.NO_ARMOR_ITEM.log(Template.of("name", prefix + "<part>"),
                    Template.of("armor_layer_file", name));
            return true;
        }
        BufferedImage image = ImageIO.read(file);
        File emissiveFile = new File(file.getPath().replace(".png", "_e.png"));
        if (emissiveFile.exists()) {
            BufferedImage emissiveImage = ImageIO.read(emissiveFile);
            image = mergeImages(image.getWidth() + emissiveImage.getWidth(),
                    image.getHeight(),
                    image, emissiveImage);
            setPixel(image.getRaster(), 2, 0, Color.fromRGB(1, 0, 0));
        }
        addPixel(image, builder, name, prefix);
        return true;
    }

    private void addPixel(BufferedImage image, ItemBuilder builder, String name, String prefix) {
        Color stuffColor = builder.getColor();
        if (usedColors.containsKey(stuffColor.asRGB())) {
            String detectedPrefix = usedColors.get(stuffColor.asRGB());
            if (!detectedPrefix.equals(prefix))
                Message.DUPLICATE_ARMOR_COLOR.log(
                        Template.of("first_armor_prefix", prefix),
                        Template.of("second_armor_prefix", detectedPrefix));
        } else usedColors.put(stuffColor.asRGB(), prefix);

        setPixel(image.getRaster(), 0, 0, stuffColor);
        if (name.contains("armor_layer_1")) {
            layers1.add(image);
            layer1Width += image.getWidth();
            if (image.getHeight() > layer1Height)
                layer1Height = image.getHeight();
        } else {
            layers2.add(image);
            layer2Width += image.getWidth();
            if (image.getHeight() > layer2Height)
                layer2Height = image.getHeight();
        }
    }

    public boolean hasCustomArmors() {
        return !(layers1.isEmpty() || layers2.isEmpty() || layer1 == null || layer2 == null);
    }

    public InputStream getLayerOne() throws IOException {
        return getInputStream(layer1Width, layer1Height, layer1, layers1);
    }

    public InputStream getLayerTwo() throws IOException {
        return getInputStream(layer2Width, layer2Height, layer2, layers2);
    }

    private InputStream getInputStream(int layerWidth, int layerHeight,
                                       BufferedImage layer, List<BufferedImage> layers) throws IOException {
        layers.add(0, layer);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(mergeImages(layerWidth, layerHeight, layers.toArray(new BufferedImage[0])),
                "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private void setPixel(WritableRaster raster, int x, int y, Color color) {
        raster.setPixel(x, y, new int[]{color.getRed(), color.getGreen(), color.getBlue(), 255});
    }

    private BufferedImage mergeImages(int width, int height, BufferedImage... images) {
        BufferedImage concatImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = concatImage.createGraphics();
        int currentWidth = 0;
        for (BufferedImage bufferedImage : images) {
            g2d.drawImage(bufferedImage, currentWidth, 0, null);
            currentWidth += bufferedImage.getWidth();
        }
        g2d.dispose();
        return concatImage;
    }


}
