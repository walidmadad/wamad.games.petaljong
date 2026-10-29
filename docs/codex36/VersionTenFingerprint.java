import com.tilebloom.game.domain.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
public class VersionTenFingerprint {
 public static void main(String[] args) throws Exception {
  for (long id: new long[]{8,12,31,75,101,1001}) {
   Object generator=Class.forName(args[0]).getField("INSTANCE").get(null);
   LevelDefinition l=(LevelDefinition)generator.getClass().getMethod("generate",long.class,boolean.class).invoke(generator,id,false);
   String value=l.getSeed()+"|"+l.getTiles()+"|"+l.getWinningSelections();
   System.out.println(id+" "+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))));
  }
 }
}
