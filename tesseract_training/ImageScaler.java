
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageScaler {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImageScaler <input> <output>");
            System.exit(1);
        }
        try {
            File input = new File(args[0]);
            BufferedImage original = ImageIO.read(input);

            // 2x Scaling + Grayscale
            int targetWidth = original.getWidth() * 2;
            int targetHeight = original.getHeight() * 2;

            BufferedImage resized = new BufferedImage(
                    targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);

            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            ImageIO.write(resized, "png", new File(args[1]));
            System.out.println("Processed: " + args[1]);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
