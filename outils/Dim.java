import javax.imageio.ImageIO; import java.io.File;
public class Dim { public static void main(String[] a) throws Exception {
  for (String d : a) { File f = new File(d);
    File[] l = f.listFiles((x,n)->n.endsWith(".png")); if (l==null) continue;
    java.util.Arrays.sort(l);
    var im = ImageIO.read(l[0]);
    System.out.printf("%s : %d fichiers, premier %s = %dx%d%n",
      f.getName(), l.length, l[0].getName(), im.getWidth(), im.getHeight()); } } }
