package com.loopers.support;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.domain.coupon.enums.CouponType;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.enums.UserRole;
import com.loopers.domain.user.vo.BirthDay;
import com.loopers.domain.user.vo.Email;
import com.loopers.domain.user.vo.Name;
import com.loopers.domain.user.vo.Password;
import com.loopers.domain.user.vo.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Profile("local")
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalDataSeeder implements ApplicationRunner {

    private static final String PASSWORD = "Test1234!";
    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PW = "Admin1234!";

    private static final List<Long>   FIXED_AMOUNTS  = List.of(3000L, 5000L, 10000L);
    private static final List<Long>   RATE_VALUES    = List.of(10L, 20L, 30L);
    private static final List<Long>   PRODUCT_PRICES = List.of(5000L, 10000L, 15000L, 20000L, 30000L);
    private static final List<String> PRODUCT_NAMES  = List.of("운동화", "청바지", "티셔츠", "가방", "모자");

    private static final int USER_COUNT = 3000; // RATE=60 x 30s x 1.2 여유분(약 2160개 token) 커버
    private static final long STOCK_QUANTITY = 100_000; // 부하테스트 중 재고소진으로 결과 왜곡되지 않도록 전 상품 동일하게 넉넉히

    private static final int BULK_BRAND_COUNT    = 20;
    //private static final int PRODUCTS_PER_BRAND  = 500_000;
    private static final int PRODUCTS_PER_BRAND  = 500;
    private static final int BATCH_CHUNK_SIZE    = 2_000;
    private static final int WISHLIST_PRODUCT_COUNT  = 1_000;
    private static final int WISHLIST_LIKE_USER_COUNT = 300;

    private static final List<String> BULK_BRAND_NAMES = List.of(
        "패션시티", "전자월드", "식품마트", "뷰티랩", "스포츠킹",
        "도서관", "가전플러스", "생활마트", "유아세상", "펫월드",
        "여행샵", "자동차몰", "건강나라", "홈데코", "문구천국",
        "게임존", "뮤직스토어", "아웃도어", "주방명가", "럭셔리몰"
    );

    private static final List<String> CATEGORY_PREFIXES = List.of(
        "프리미엄", "베이직", "스탠다드", "클래식", "모던",
        "빈티지", "미니멀", "럭셔리", "에코", "스마트"
    );

    private static final List<String> PRODUCT_KEYWORDS = List.of(
        "셔츠", "바지", "원피스", "재킷", "코트", "스니커즈", "부츠", "샌들", "슬리퍼", "모자",
        "가방", "지갑", "벨트", "시계", "반지", "목걸이", "팔찌", "귀걸이", "선글라스", "스카프",
        "이어폰", "헤드폰", "스피커", "키보드", "마우스", "모니터", "태블릿", "스마트워치", "충전기", "케이블",
        "쌀", "라면", "과자", "음료", "커피", "차", "주스", "빵", "케이크", "아이스크림",
        "샴푸", "린스", "바디워시", "로션", "크림", "립스틱", "파운데이션", "마스카라", "향수", "세럼",
        "요가매트", "덤벨", "밴드", "자전거", "헬멧", "수영복", "테니스라켓", "골프공", "등산화", "배드민턴",
        "소설", "만화", "교재", "문제집", "잡지", "사전", "요리책", "여행책", "자기계발서", "아동서",
        "냉장고", "세탁기", "에어컨", "청소기", "전자레인지", "오븐", "식기세척기", "공기청정기", "제습기", "가습기",
        "기저귀", "분유", "유아복", "장난감", "유모차", "아기띠", "이유식", "젖병", "딸랑이", "안전문",
        "사료", "간식", "장난감볼", "목줄", "하네스", "샴푸", "빗", "침대", "집", "의류"
    );

    private final UserRepository userRepository;
    private final BrandFacade brandFacade;
    private final ProductFacade productFacade;
    private final CouponFacade couponFacade;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
        seedUsers();
        seedProducts();
        seedBulkProducts();
        seedCoupons();
        seedWishlists();
        log.info("[LocalDataSeeder] 시드 데이터 생성 완료");
    }

    private void seedAdminUser() {
        try {
            userRepository.save(new UserModel(
                    new UserId(ADMIN_ID),
                    new Password(passwordEncoder.encode(ADMIN_PW)),
                    new Name("관리자"),
                    new BirthDay("1990-01-01"),
                    new Email("admin@test.com"),
                    UserRole.ADMIN
            ));
            log.info("[LocalDataSeeder] 어드민 유저 생성: {}", ADMIN_ID);
        } catch (Exception e) {
            log.info("[LocalDataSeeder] 어드민 유저 이미 존재, 스킵");
        }
    }

    private void seedUsers() {
        try {
            Long existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE userid LIKE 'testuser%'", Long.class);
            if (existingCount != null && existingCount >= USER_COUNT) {
                log.info("[LocalDataSeeder] 테스트 유저 이미 충분히 존재, 스킵");
                return;
            }

            String hash = passwordEncoder.encode(PASSWORD);
            String now = ZonedDateTime.now().format(DATETIME_FORMAT);
            List<Object[]> rows = new ArrayList<>(USER_COUNT);
            for (int i = 1; i <= USER_COUNT; i++) {
                rows.add(new Object[]{
                    "testuser" + i, hash, "테스트유저" + i, "1990-01-01",
                    "testuser" + i + "@test.com", "USER", now, now
                });
            }
            jdbcTemplate.batchUpdate(
                "INSERT IGNORE INTO users (userid, password, name, birth_day, email, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                rows
            );
            log.info("[LocalDataSeeder] 테스트 유저 {}명 생성 완료 (비밀번호 해시 1회 계산 후 재사용)", USER_COUNT);
        } catch (Exception e) {
            log.warn("[LocalDataSeeder] 테스트 유저 생성 스킵: {}", e.getMessage());
        }
    }

    private void seedProducts() {
        try {
            BrandInfo brand = brandFacade.register("시드브랜드");
            for (int i = 0; i < PRODUCT_NAMES.size(); i++) {
                long price = PRODUCT_PRICES.get(i);
                productFacade.registerProduct(brand.id(), PRODUCT_NAMES.get(i), price, (int) STOCK_QUANTITY);
                log.info("[LocalDataSeeder] 상품 생성: {} ({}원)", PRODUCT_NAMES.get(i), price);
            }
        } catch (Exception e) {
            log.info("[LocalDataSeeder] 상품 생성 스킵: {}", e.getMessage());
        }
    }

    private void seedCoupons() {
        for (int i = 1; i <= 10; i++) {
            try {
                CouponType type = (i % 2 == 0) ? CouponType.RATE : CouponType.FIXED;
                long value = (type == CouponType.FIXED)
                        ? pick(FIXED_AMOUNTS)
                        : pick(RATE_VALUES);
                String name = (type == CouponType.FIXED)
                        ? "정액쿠폰-" + value + "원"
                        : "정률쿠폰-" + value + "%";

                CouponInfo coupon = couponFacade.create(name, type, value, null, ZonedDateTime.now().plusDays(30), 100L);

                /* Long userId = userRepository.findByUserId(new UserId("testuser" + i)).orElseThrow().getId();
                couponFacade.issue(coupon.id(), userId);
                log.info("[LocalDataSeeder] 쿠폰 발급: testuser{} ← {} ({})", i, name, type);*/
            } catch (Exception e) {
                log.info("[LocalDataSeeder] 쿠폰 생성/발급 스킵: {}", e.getMessage());
            }
        }
    }

    private void seedBulkProducts() {
        try {
            Long existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM brands WHERE name IN (" +
                BULK_BRAND_NAMES.stream().map(n -> "'" + n + "'").reduce((a, b) -> a + "," + b).orElse("''") + ")",
                Long.class
            );
            if (existingCount != null && existingCount > 0) {
                log.info("[LocalDataSeeder] 대량 상품 데이터 이미 존재, 스킵");
                return;
            }

            log.info("[LocalDataSeeder] 대량 상품 데이터 생성 시작 ({}개 브랜드 × {}개 상품)",
                BULK_BRAND_COUNT, PRODUCTS_PER_BRAND);

            List<Long> brandIds = new ArrayList<>();
            for (String brandName : BULK_BRAND_NAMES) {
                BrandInfo brand = brandFacade.register(brandName);
                brandIds.add(brand.id());
            }

            String now = ZonedDateTime.now().format(DATETIME_FORMAT);

            for (int b = 0; b < brandIds.size(); b++) {
                Long brandId = brandIds.get(b);
                String brandName = BULK_BRAND_NAMES.get(b);

                for (int chunkStart = 1; chunkStart <= PRODUCTS_PER_BRAND; chunkStart += BATCH_CHUNK_SIZE) {
                    int chunkEnd = Math.min(chunkStart + BATCH_CHUNK_SIZE - 1, PRODUCTS_PER_BRAND);
                    List<Object[]> chunk = new ArrayList<>(chunkEnd - chunkStart + 1);
                    for (int i = chunkStart; i <= chunkEnd; i++) {
                        String prefix = CATEGORY_PREFIXES.get(random.nextInt(CATEGORY_PREFIXES.size()));
                        String keyword = PRODUCT_KEYWORDS.get(random.nextInt(PRODUCT_KEYWORDS.size()));
                        String name = prefix + " " + keyword + " " + String.format("%06d", i);
                        chunk.add(new Object[]{brandId, name, "ACTIVE", now, now});
                    }
                    jdbcTemplate.batchUpdate(
                        "INSERT IGNORE INTO products (brand_id, name, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                        chunk
                    );
                }

                jdbcTemplate.update(
                    "INSERT INTO product_stocks (product_id, price, stock_quantity, created_at, updated_at) " +
                    "SELECT p.id, " +
                    "  ELT(FLOOR(RAND()*20)+1," +
                    "    1000,2000,3000,5000,8000,10000,15000,20000,25000,30000," +
                    "    40000,50000,60000,70000,80000,100000,120000,150000,200000,300000), " +
                    "  ?, ?, ? " +
                    "FROM products p " +
                    "LEFT JOIN product_stocks ps ON ps.product_id = p.id " +
                    "WHERE p.brand_id = ? AND ps.id IS NULL",
                    STOCK_QUANTITY, now, now, brandId
                );

                jdbcTemplate.update(
                    "UPDATE products p " +
                    "INNER JOIN (SELECT product_id, MIN(price) AS min_price FROM product_stocks GROUP BY product_id) ps " +
                    "ON p.id = ps.product_id " +
                    "SET p.min_price = ps.min_price " +
                    "WHERE p.brand_id = ?",
                    brandId
                );

                log.info("[LocalDataSeeder] 브랜드 '{}' 상품 {}개 생성 완료", brandName, PRODUCTS_PER_BRAND);
            }

            log.info("[LocalDataSeeder] 대량 상품 데이터 생성 완료: 총 {}개",
                (long) BULK_BRAND_COUNT * PRODUCTS_PER_BRAND);
        } catch (Exception e) {
            log.warn("[LocalDataSeeder] 대량 상품 생성 스킵: {}", e.getMessage());
        }
    }

    private void seedWishlists() {
        try {
            Long existingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wishlists", Long.class);
            if (existingCount != null && existingCount > 0) {
                log.info("[LocalDataSeeder] 위시리스트 데이터 이미 존재, 스킵");
                return;
            }

            String now = ZonedDateTime.now().format(DATETIME_FORMAT);
            String dummyPw = passwordEncoder.encode("Like1234!");
            List<Object[]> likeUserRows = new ArrayList<>(WISHLIST_LIKE_USER_COUNT);
            for (int i = 1; i <= WISHLIST_LIKE_USER_COUNT; i++) {
                likeUserRows.add(new Object[]{
                    "likeuser" + i, dummyPw, "좋아요유저" + i, "1990-01-01",
                    "likeuser" + i + "@seed.com", "USER", now, now
                });
            }
            jdbcTemplate.batchUpdate(
                "INSERT IGNORE INTO users (userid, password, name, birth_day, email, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                likeUserRows
            );
            log.info("[LocalDataSeeder] 좋아요 시드 유저 {}명 생성", WISHLIST_LIKE_USER_COUNT);

            Long userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE userid LIKE 'likeuser%'", Long.class);
            if (userCount == null || userCount == 0) {
                log.info("[LocalDataSeeder] 위시리스트 시드용 유저 없음, 스킵");
                return;
            }

            Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
            if (productCount == null || productCount == 0) {
                log.info("[LocalDataSeeder] 위시리스트 시드용 상품 없음, 스킵");
                return;
            }

            int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO wishlists (user_id, product_id, created_at, updated_at) " +
                "SELECT u.id, p.id, ?, ? " +
                "FROM users u " +
                "CROSS JOIN (SELECT id FROM products ORDER BY id ASC LIMIT ?) p " +
                "WHERE u.userid LIKE 'likeuser%'",
                now, now, WISHLIST_PRODUCT_COUNT
            );
            log.info("[LocalDataSeeder] 위시리스트 {}개 생성 완료 (상위 {}개 상품 × {}명)", inserted, WISHLIST_PRODUCT_COUNT, userCount);

            jdbcTemplate.update(
                "UPDATE products p " +
                "INNER JOIN (SELECT product_id, COUNT(*) AS cnt FROM wishlists GROUP BY product_id) w " +
                "ON p.id = w.product_id " +
                "SET p.like_count = w.cnt"
            );
            log.info("[LocalDataSeeder] 상품 좋아요 카운트 동기화 완료");
        } catch (Exception e) {
            log.warn("[LocalDataSeeder] 위시리스트 생성 스킵: {}", e.getMessage());
        }
    }

    private long pick(List<Long> values) {
        return values.get(random.nextInt(values.size()));
    }
}
