import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class VerifyBcrypt {
  public static void main(String[] args) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    String hash = "$2a$12$K3L/Dh2mBJy4GpVfEw.9WOBGJzxI8rFhEiGP/wCKH9L3MidG9oqAS";
    System.out.println("GoldenHeart@2026=" + encoder.matches("GoldenHeart@2026", hash));
    System.out.println("Admin123=" + encoder.matches("Admin123", hash));
    System.out.println("newHash=" + encoder.encode("GoldenHeart@2026"));
  }
}
