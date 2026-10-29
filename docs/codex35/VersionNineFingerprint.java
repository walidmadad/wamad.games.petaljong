import com.tilebloom.game.domain.*;
public class VersionNineFingerprint {
 public static void main(String[] args) throws Exception {
  Class<?> generator;
  try { generator=Class.forName("com.tilebloom.game.domain.VersionNineGenerator"); }
  catch(ClassNotFoundException e) { generator=Class.forName("com.tilebloom.game.domain.EndlessGenerator"); }
  Object instance=generator.getField("INSTANCE").get(null);
  for(long id:new long[]{8,12,31,75,101,1001}) {
   LevelDefinition l=(LevelDefinition)generator.getMethod("generate",long.class,boolean.class).invoke(instance,id,false);
   StringBuilder s=new StringBuilder();
   for(Tile t:l.getTiles()) s.append(t.getId()).append(':').append(t.getSymbol().getStableId()).append(':').append(t.getX()).append(':').append(t.getY()).append(':').append(t.getLayer()).append(';');
   s.append(l.getWinningSelections());
   byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
   System.out.println(id+","+l.getSeed()+","+java.util.HexFormat.of().formatHex(digest));
  }
 }
}
