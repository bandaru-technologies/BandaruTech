package com.anyneeds.config;

import com.anyneeds.category.entity.Category;
import com.anyneeds.category.repository.CategoryRepository;
import com.anyneeds.listing.entity.Listing;
import com.anyneeds.listing.repository.ListingRepository;
import com.anyneeds.user.entity.User;
import com.anyneeds.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "salepe.demo.seed", havingValue = "true")
@Order(2)
@Slf4j
public class DemoListingSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;

    private final Random random = new Random(42L);

    @Override
    @Transactional
    public void run(String... args) {
        log.warn("salepe.demo.seed=true — wiping all listings and reseeding demo data.");
        listingRepository.deleteAll();
        List<User> sellers = upsertDemoSellers();
        Map<String, Category> bySlug = loadCategoriesBySlug();
        List<Listing> listings = buildListings(bySlug, sellers);
        listingRepository.saveAll(listings);
        log.info("Seeded {} demo listings across {} categories.", listings.size(), bySlug.size());
    }

    private Map<String, Category> loadCategoriesBySlug() {
        Map<String, Category> bySlug = new HashMap<>();
        for (Category c : categoryRepository.findAll()) bySlug.put(c.getSlug(), c);
        return bySlug;
    }

    private List<User> upsertDemoSellers() {
        String[][] sellers = {
            {"9000000001", "Rahul Sharma"},
            {"9000000002", "Priya Iyer"},
            {"9000000003", "Arjun Reddy"},
            {"9000000004", "Anjali Patel"},
            {"9000000005", "Vikram Singh"},
            {"9000000006", "Sneha Nair"},
            {"9000000007", "Karthik Rao"},
            {"9000000008", "Meera Kapoor"},
            {"9000000009", "Rohit Verma"},
            {"9000000010", "Divya Menon"},
            {"9000000011", "Aditya Joshi"},
            {"9000000012", "Pooja Desai"}
        };
        List<User> result = new ArrayList<>();
        for (String[] s : sellers) {
            Optional<User> existing = userRepository.findByPhoneNumber(s[0]);
            result.add(existing.orElseGet(() -> userRepository.save(
                User.builder().phoneNumber(s[0]).name(s[1]).build()
            )));
        }
        return result;
    }

    private List<Listing> buildListings(Map<String, Category> bySlug, List<User> sellers) {
        List<Listing> all = new ArrayList<>();
        addBatch(all, bySlug.get("mobiles-electronics"), sellers, MOBILES_ELECTRONICS, 25);
        addBatch(all, bySlug.get("cars"), sellers, CARS, 15);
        addBatch(all, bySlug.get("bikes"), sellers, BIKES, 15);
        addBatch(all, bySlug.get("furniture-home"), sellers, FURNITURE_HOME, 20);
        addBatch(all, bySlug.get("real-estate"), sellers, REAL_ESTATE, 15);
        addBatch(all, bySlug.get("hotel-pg"), sellers, HOTEL_PG, 5);
        addBatch(all, bySlug.get("fashion-beauty"), sellers, FASHION_BEAUTY, 15);
        addBatch(all, bySlug.get("jobs"), sellers, JOBS, 15);
        addBatch(all, bySlug.get("books-education"), sellers, BOOKS_EDUCATION, 10);
        addBatch(all, bySlug.get("animals-pets"), sellers, ANIMALS_PETS, 5);
        addBatch(all, bySlug.get("others"), sellers, OTHERS, 10);
        return all;
    }

    private void addBatch(List<Listing> out, Category category, List<User> sellers,
                          ListingTemplate[] templates, int count) {
        if (category == null) return;
        for (int i = 0; i < count; i++) {
            ListingTemplate t = templates[random.nextInt(templates.length)];
            CityLocality loc = CITY_LOCALITIES[random.nextInt(CITY_LOCALITIES.length)];
            String locality = loc.localities[random.nextInt(loc.localities.length)];
            BigDecimal price = t.priceMin == null ? null
                : BigDecimal.valueOf(t.priceMin + random.nextInt(Math.max(1, t.priceMax - t.priceMin)));
            Listing.ListingCondition condition = t.allowedConditions == null ? null
                : t.allowedConditions[random.nextInt(t.allowedConditions.length)];
            User seller = sellers.get(random.nextInt(sellers.size()));
            String imageSeed = category.getSlug() + "-" + (out.size() + 1);
            out.add(Listing.builder()
                .title(t.title)
                .description(t.description)
                .price(price)
                .category(category)
                .user(seller)
                .location(locality + ", " + loc.city)
                .city(loc.city)
                .state(loc.state)
                .condition(condition)
                .negotiable(random.nextBoolean())
                .viewCount((long) random.nextInt(500))
                .imageUrls(new ArrayList<>(List.of(
                    "https://picsum.photos/seed/" + imageSeed + "-1/640/480",
                    "https://picsum.photos/seed/" + imageSeed + "-2/640/480"
                )))
                .status(Listing.ListingStatus.ACTIVE)
                .build());
        }
    }

    private static final Listing.ListingCondition[] USED_ALL = {
        Listing.ListingCondition.LIKE_NEW, Listing.ListingCondition.GOOD, Listing.ListingCondition.FAIR
    };
    private static final Listing.ListingCondition[] USED_OR_NEW = {
        Listing.ListingCondition.NEW, Listing.ListingCondition.LIKE_NEW, Listing.ListingCondition.GOOD
    };

    private record ListingTemplate(String title, String description,
                                   Integer priceMin, Integer priceMax,
                                   Listing.ListingCondition[] allowedConditions) {}

    private record CityLocality(String city, String state, String[] localities) {}

    private static final CityLocality[] CITY_LOCALITIES = {
        new CityLocality("Bangalore", "Karnataka", new String[]{
            "Koramangala", "Indiranagar", "HSR Layout", "Whitefield", "Jayanagar",
            "Marathahalli", "Electronic City", "Hebbal", "Bellandur", "JP Nagar"}),
        new CityLocality("Mumbai", "Maharashtra", new String[]{
            "Andheri West", "Bandra", "Powai", "Thane West", "Borivali", "Goregaon", "Malad"}),
        new CityLocality("Delhi", "Delhi", new String[]{
            "Saket", "Dwarka", "Rohini", "Lajpat Nagar", "Karol Bagh", "Vasant Kunj"}),
        new CityLocality("Gurgaon", "Haryana", new String[]{
            "DLF Phase 3", "Sector 56", "Golf Course Road", "Cyber City", "Sohna Road"}),
        new CityLocality("Noida", "Uttar Pradesh", new String[]{
            "Sector 18", "Sector 62", "Sector 137", "Greater Noida West"}),
        new CityLocality("Hyderabad", "Telangana", new String[]{
            "Madhapur", "Gachibowli", "Banjara Hills", "Hitech City", "Kondapur", "Kukatpally"}),
        new CityLocality("Chennai", "Tamil Nadu", new String[]{
            "T. Nagar", "Velachery", "Anna Nagar", "Adyar", "OMR", "Porur"}),
        new CityLocality("Pune", "Maharashtra", new String[]{
            "Koregaon Park", "Baner", "Hinjewadi", "Viman Nagar", "Kothrud", "Wakad"}),
        new CityLocality("Kolkata", "West Bengal", new String[]{
            "Salt Lake", "New Town", "Park Street", "Ballygunge", "Howrah"}),
        new CityLocality("Ahmedabad", "Gujarat", new String[]{
            "Satellite", "Bopal", "Vastrapur", "Maninagar", "Prahlad Nagar"}),
        new CityLocality("Jaipur", "Rajasthan", new String[]{
            "C-Scheme", "Vaishali Nagar", "Malviya Nagar", "Mansarovar"}),
        new CityLocality("Lucknow", "Uttar Pradesh", new String[]{
            "Hazratganj", "Gomti Nagar", "Aliganj", "Indira Nagar"}),
        new CityLocality("Chandigarh", "Chandigarh", new String[]{
            "Sector 17", "Sector 22", "Sector 35", "Mohali", "Panchkula"}),
        new CityLocality("Indore", "Madhya Pradesh", new String[]{
            "Vijay Nagar", "Palasia", "Bhanwarkua", "Bengali Square"}),
        new CityLocality("Coimbatore", "Tamil Nadu", new String[]{
            "RS Puram", "Saibaba Colony", "Peelamedu", "Race Course"}),
        new CityLocality("Kochi", "Kerala", new String[]{
            "Kakkanad", "Fort Kochi", "Edappally", "Kaloor"}),
        new CityLocality("Bhubaneswar", "Odisha", new String[]{
            "Patia", "Saheed Nagar", "Jaydev Vihar", "Chandrasekharpur"})
    };

    private static final ListingTemplate[] MOBILES_ELECTRONICS = {
        new ListingTemplate("iPhone 15 Pro Max 256GB Natural Titanium",
            "Excellent condition, 95% battery health, all original accessories included.", 95000, 125000, USED_OR_NEW),
        new ListingTemplate("iPhone 14 128GB Blue",
            "1 year old, scratchless, with original box, charger and bill.", 45000, 60000, USED_ALL),
        new ListingTemplate("iPhone 13 256GB Midnight",
            "Battery 89%, screen guard and case included, single owner.", 38000, 52000, USED_ALL),
        new ListingTemplate("Samsung Galaxy S24 Ultra 512GB",
            "Mint condition, S Pen working perfectly, no scratches, full box.", 80000, 105000, USED_OR_NEW),
        new ListingTemplate("OnePlus 12R 256GB Cool Blue",
            "6 months old, in warranty, original packaging available.", 32000, 42000, USED_OR_NEW),
        new ListingTemplate("Google Pixel 8 Pro 128GB Obsidian",
            "Imported, sealed pack, with India compatible charger.", 55000, 72000, USED_OR_NEW),
        new ListingTemplate("MacBook Air M2 13-inch 256GB Midnight",
            "AppleCare+ valid until 2026, used lightly for college work.", 75000, 95000, USED_OR_NEW),
        new ListingTemplate("Dell XPS 15 i7 16GB 512GB",
            "Used for remote work, well maintained, fresh Windows install.", 60000, 85000, USED_ALL),
        new ListingTemplate("HP Pavilion Gaming Laptop i5 16GB RTX 3050",
            "Great for college + casual gaming, 1.5 years old.", 45000, 62000, USED_ALL),
        new ListingTemplate("iPad Pro 11\" M2 128GB Wi-Fi",
            "With Apple Pencil 2nd gen, used for digital art.", 55000, 75000, USED_OR_NEW),
        new ListingTemplate("Sony WH-1000XM5 Wireless Noise Cancelling Headphones",
            "Bought 4 months ago, like new, with case and cable.", 18000, 26000, USED_OR_NEW),
        new ListingTemplate("Apple Watch Series 9 GPS 45mm",
            "All bands included, screen protector applied since unboxing.", 28000, 38000, USED_OR_NEW),
        new ListingTemplate("Samsung 55\" Crystal 4K UHD Smart TV",
            "Less than a year old, with original remote and box.", 32000, 45000, USED_ALL),
        new ListingTemplate("Sony Bravia 43\" 4K Google TV",
            "Excellent picture, used in a guest bedroom, rarely watched.", 28000, 40000, USED_OR_NEW),
        new ListingTemplate("PlayStation 5 Disc Edition + 2 Controllers + 3 Games",
            "Spider-Man 2, GTA V, FIFA 24 included. Original packaging.", 38000, 50000, USED_OR_NEW),
        new ListingTemplate("Xbox Series X 1TB",
            "Single owner, gentle usage, with Game Pass remaining.", 35000, 45000, USED_ALL),
        new ListingTemplate("DJI Mini 3 Pro Drone Fly More Combo",
            "Used twice for a wedding shoot. With ND filters and bag.", 55000, 75000, USED_OR_NEW),
        new ListingTemplate("Canon EOS R10 with 18-45mm Kit Lens",
            "Mirrorless camera in excellent condition, ~3000 shutter count.", 60000, 80000, USED_OR_NEW)
    };

    private static final ListingTemplate[] CARS = {
        new ListingTemplate("Maruti Swift VXi 2021 Petrol",
            "Single owner, 32,000 km, all service records, accident-free.", 550000, 720000, USED_ALL),
        new ListingTemplate("Hyundai Creta SX 2020 Diesel",
            "First owner, 48,000 km, sunroof, leather seats, fully loaded.", 1050000, 1350000, USED_ALL),
        new ListingTemplate("Honda City ZX 2019 Petrol CVT",
            "Top-end variant, well maintained, 55,000 km, second owner.", 850000, 1050000, USED_ALL),
        new ListingTemplate("Tata Nexon XZ+ 2022 Diesel",
            "Showroom condition, 22,000 km, sunroof, ventilated seats.", 950000, 1200000, USED_ALL),
        new ListingTemplate("Kia Seltos GTX+ 2021 Petrol Turbo",
            "Single owner, 40,000 km, panoramic sunroof, JBL speakers.", 1300000, 1600000, USED_ALL),
        new ListingTemplate("Mahindra XUV700 AX7 2023 Diesel AT",
            "7-seater, 18,000 km, ADAS, panoramic roof, top variant.", 2200000, 2700000, USED_OR_NEW),
        new ListingTemplate("Maruti Baleno Alpha 2022 Petrol AMT",
            "Sparingly used, 15,000 km, full insurance, head-up display.", 750000, 920000, USED_ALL),
        new ListingTemplate("Toyota Innova Crysta ZX 2019 Diesel",
            "Family-driven, captain seats, 78,000 km, regular servicing.", 1700000, 2000000, USED_ALL),
        new ListingTemplate("Skoda Slavia 1.5 TSI 2023",
            "Top variant, virtual cockpit, 12,000 km, accident-free.", 1500000, 1850000, USED_OR_NEW),
        new ListingTemplate("MG Hector Plus 2021 Diesel",
            "7-seater, panoramic sunroof, 38,000 km, single owner.", 1500000, 1850000, USED_ALL)
    };

    private static final ListingTemplate[] BIKES = {
        new ListingTemplate("Royal Enfield Classic 350 2022",
            "Single owner, 14,000 km, all original parts, fresh service done.", 145000, 175000, USED_ALL),
        new ListingTemplate("Royal Enfield Hunter 350 2023",
            "Showroom condition, 6,000 km, retro mod variant.", 155000, 185000, USED_OR_NEW),
        new ListingTemplate("KTM Duke 390 2021",
            "Adult-owned, 22,000 km, exhaust upgraded, new tyres.", 200000, 250000, USED_ALL),
        new ListingTemplate("Bajaj Pulsar NS200 2022",
            "Single owner, 18,000 km, full insurance and bill copy.", 110000, 135000, USED_ALL),
        new ListingTemplate("Yamaha MT-15 V2 2023",
            "10,000 km, full service history, new tyres and chain sprocket.", 130000, 155000, USED_ALL),
        new ListingTemplate("TVS Apache RTR 200 4V 2021",
            "Well-maintained, 28,000 km, USD forks, new battery.", 95000, 115000, USED_ALL),
        new ListingTemplate("Honda Activa 6G 2022 Scooter",
            "Lady-driven, 9,000 km, mint condition, full insurance.", 65000, 80000, USED_ALL),
        new ListingTemplate("Ather 450X Gen 3 Electric Scooter",
            "Bought 2023, 7,000 km, original battery, fast charger included.", 95000, 120000, USED_ALL),
        new ListingTemplate("Suzuki Gixxer SF 250 2021",
            "Single owner, 20,000 km, very clean, all documents valid.", 130000, 155000, USED_ALL),
        new ListingTemplate("Kawasaki Ninja 300 2020 ABS",
            "Adult-driven, 16,000 km, accident-free, recent service.", 280000, 340000, USED_ALL)
    };

    private static final ListingTemplate[] FURNITURE_HOME = {
        new ListingTemplate("L-shaped Fabric Sofa Set 6-Seater (Beige)",
            "Bought from Wakefit, 1.5 years old, light usage, no stains.", 18000, 32000, USED_ALL),
        new ListingTemplate("Solid Wood Queen Size Bed with Storage",
            "Sheesham wood, hydraulic storage, less than 2 years old.", 15000, 28000, USED_ALL),
        new ListingTemplate("6-Seater Dining Table with Cushioned Chairs",
            "Teak finish, solid wood, marble top table.", 14000, 25000, USED_ALL),
        new ListingTemplate("Godrej Almirah 4 Door Steel Wardrobe",
            "Well-maintained, with locker compartment, recent paint touch-up.", 8000, 14000, USED_ALL),
        new ListingTemplate("LG 1.5 Ton 5-Star Inverter Split AC",
            "Bought in 2022, used only one summer, with warranty card.", 22000, 32000, USED_ALL),
        new ListingTemplate("Voltas 1 Ton Split AC (3-Star)",
            "Working perfectly, used in a guest room, with installation kit.", 14000, 22000, USED_ALL),
        new ListingTemplate("Samsung 253L Double Door Refrigerator",
            "Frost-free, energy efficient, less than 3 years old.", 14000, 24000, USED_ALL),
        new ListingTemplate("Whirlpool 7kg Front Load Washing Machine",
            "Working perfectly, with original installation manual.", 12000, 22000, USED_ALL),
        new ListingTemplate("Recliner Single Seater (Brown Leatherette)",
            "Manual recliner, used in a study room, in great shape.", 8000, 14000, USED_ALL),
        new ListingTemplate("Study Desk + Office Chair Combo",
            "Engineered wood desk, ergonomic mesh chair, work-from-home ready.", 6000, 12000, USED_ALL),
        new ListingTemplate("Bean Bag XXXL with Beans (Royal Blue)",
            "Brand new beans refilled last month, like new.", 1500, 3500, USED_ALL),
        new ListingTemplate("Modular Kitchen Trolley Set (6 Drawers)",
            "Stainless steel, removed from a renovated home.", 8000, 16000, USED_ALL)
    };

    private static final ListingTemplate[] REAL_ESTATE = {
        new ListingTemplate("2 BHK Furnished Flat for Rent",
            "Semi-furnished, family preferred, 2-wheeler parking, society maintenance included.", 22000, 45000, null),
        new ListingTemplate("3 BHK Apartment with Balcony — Immediate Possession",
            "East-facing, modular kitchen, gated community with gym and pool.", 35000, 70000, null),
        new ListingTemplate("1 BHK Apartment for Rent — Bachelors Welcome",
            "Furnished with bed, wardrobe, AC. Walking distance to metro.", 12000, 28000, null),
        new ListingTemplate("1 RK Studio for Rent",
            "Fully furnished, ideal for working professionals.", 8000, 16000, null),
        new ListingTemplate("3 BHK Independent Villa for Sale",
            "1800 sqft, 3 bedrooms, gated layout with 24x7 security.", 12500000, 22000000, null),
        new ListingTemplate("Commercial Office Space 800 sqft",
            "Ready-to-move office space with cabin layout, suitable for startups.", 50000, 95000, null),
        new ListingTemplate("Open Plot for Sale — 1200 sqft",
            "Clear title, BMRDA approved, gated layout.", 4500000, 9000000, null)
    };

    private static final ListingTemplate[] HOTEL_PG = {
        new ListingTemplate("PG for Men — Single Occupancy with Food",
            "AC room, Wi-Fi, breakfast and dinner included. Walking distance to tech park.", 9000, 18000, null),
        new ListingTemplate("PG for Women — Twin Sharing",
            "Safe, well-maintained, with CCTV and warden. All meals included.", 8000, 15000, null),
        new ListingTemplate("Co-living Single Room with Attached Bathroom",
            "Fully furnished, daily cleaning, weekly laundry.", 12000, 22000, null),
        new ListingTemplate("Service Apartment for Short Stay",
            "1 BHK fully furnished, daily / weekly / monthly stay available.", 1500, 3500, null)
    };

    private static final ListingTemplate[] FASHION_BEAUTY = {
        new ListingTemplate("Manyavar Sherwani — Size 40 (Worn Once)",
            "Royal blue, with stole and shoes, bought for a wedding.", 6000, 12000, USED_ALL),
        new ListingTemplate("Sabyasachi-style Bridal Lehenga — Size M",
            "Worn for reception only, dry cleaned, all dupatta and blouse included.", 25000, 60000, USED_ALL),
        new ListingTemplate("Designer Saree Collection — 5 Pieces",
            "Banarasi silk, Kanchipuram and georgette. Festival-ready.", 8000, 22000, USED_ALL),
        new ListingTemplate("Ray-Ban Aviator Original Sunglasses",
            "100% original, with case and cleaning cloth, light scratch on one lens.", 4500, 7500, USED_ALL),
        new ListingTemplate("Fossil Gen 6 Smartwatch (Black)",
            "Box included, original charger and 2 extra straps.", 9000, 16000, USED_ALL),
        new ListingTemplate("Adidas Ultraboost 22 — Size 9 (Men's)",
            "Worn 3-4 times, original box, fresh from a regretted impulse buy.", 7000, 12000, USED_ALL),
        new ListingTemplate("Nike Air Force 1 White — Size 8",
            "Original, with box and bill, used only twice.", 5500, 9500, USED_OR_NEW),
        new ListingTemplate("Daniel Wellington Watch — Petite Melrose",
            "Classic rose gold finish, with strap and box.", 3500, 7000, USED_ALL)
    };

    private static final ListingTemplate[] JOBS = {
        new ListingTemplate("Hiring: Full Stack Developer (React + Spring Boot)",
            "2-4 years experience, hybrid in office (3 days/week), competitive salary based on experience.", null, null, null),
        new ListingTemplate("Hiring: Sales Executive — Field Sales",
            "Freshers welcome, fixed salary + incentives. Two-wheeler required.", null, null, null),
        new ListingTemplate("Hiring: Cook / Chef for Cloud Kitchen",
            "Experience in North Indian cuisine, daily wages or monthly basis. Day shift.", null, null, null),
        new ListingTemplate("Hiring: Driver (Cab / Personal)",
            "Min 3 years driving experience, valid license, knows city routes well.", null, null, null),
        new ListingTemplate("Hiring: Receptionist for Salon",
            "Female candidates only, customer-facing role. Walk-in interview Mon-Sat.", null, null, null),
        new ListingTemplate("Hiring: Delivery Partner (Two-Wheeler)",
            "Daily payouts, work 4-12 hours flexible. Bring own bike and smartphone.", null, null, null),
        new ListingTemplate("Hiring: Digital Marketing Intern (Remote)",
            "6 month internship, stipend ₹15K-25K based on skill, work-from-home.", null, null, null),
        new ListingTemplate("Hiring: Maid / House Help — Live-in",
            "Family of 4, cooking + cleaning, accommodation provided. References required.", null, null, null),
        new ListingTemplate("Hiring: Yoga Instructor — Weekend Classes",
            "Certified, must be available Saturday and Sunday mornings.", null, null, null),
        new ListingTemplate("Hiring: Accountant with Tally Knowledge",
            "Min 1 year experience, B.Com graduates preferred.", null, null, null)
    };

    private static final ListingTemplate[] BOOKS_EDUCATION = {
        new ListingTemplate("JEE Main + Advanced Complete Study Material",
            "Allen, Resonance and FIITJEE booklets, used in 2023. Great condition.", 2500, 6000, USED_ALL),
        new ListingTemplate("NEET PG Marrow Notes (All Subjects)",
            "Color-coded handwritten notes, 2024 edition.", 3500, 7000, USED_ALL),
        new ListingTemplate("CAT Quant Aptitude Preparation Books",
            "Arun Sharma, Sarvesh Verma, Pearson — full set with practice tests.", 1500, 3500, USED_ALL),
        new ListingTemplate("UPSC GS Mains 4-Booklet Set",
            "Vision IAS and Shankar IAS notes, current affairs till 2025.", 3000, 7000, USED_ALL),
        new ListingTemplate("Engineering Textbooks — 1st & 2nd Year (CSE)",
            "Galvin OS, Forouzan Networks, Cormen DSA, all in good condition.", 1500, 4000, USED_ALL),
        new ListingTemplate("CA Foundation Study Material (ICAI + Tally)",
            "All 4 subjects, latest syllabus, mock test papers included.", 2000, 4500, USED_ALL),
        new ListingTemplate("Class 10 CBSE Reference Books Combo",
            "RD Sharma, RS Aggarwal, All-in-One — for Maths, Science, English.", 800, 2200, USED_ALL),
        new ListingTemplate("GATE Computer Science Made Easy Books (2024)",
            "Full set, lightly used, with handwritten margin notes.", 3500, 7000, USED_ALL)
    };

    private static final ListingTemplate[] ANIMALS_PETS = {
        new ListingTemplate("Labrador Puppy — KCI Certified",
            "Male, 2 months old, dewormed and vaccinated, healthy and playful.", 12000, 25000, null),
        new ListingTemplate("Golden Retriever Puppies (Litter of 4)",
            "8 weeks old, with show-quality lineage, deworming and shots done.", 25000, 45000, null),
        new ListingTemplate("Persian Cat Kitten — Female (3 months)",
            "Vaccinated, litter-trained, very affectionate.", 8000, 18000, null),
        new ListingTemplate("Aquarium Fish Tank Setup (60L) with Fish",
            "Complete setup with filter, lights, gravel and 10 fish.", 2500, 6000, null),
        new ListingTemplate("Lovebirds Pair with Cage",
            "Healthy, 1 year old breeding pair with stainless steel cage.", 2000, 5000, null)
    };

    private static final ListingTemplate[] OTHERS = {
        new ListingTemplate("Decathlon Btwin Cycle 27.5\" 21-Gear",
            "Lightly used, perfect for commute or weekend rides.", 8000, 14000, USED_ALL),
        new ListingTemplate("Yamaha F310 Acoustic Guitar with Bag",
            "Beginner-friendly, with picks and tuner included.", 5000, 9000, USED_ALL),
        new ListingTemplate("Casio CTK-3500 Keyboard 61 Keys",
            "Bought during lockdown, lightly used, with stand and adapter.", 6000, 11000, USED_ALL),
        new ListingTemplate("Bullworker Steel Bow — Home Workout Equipment",
            "Compact and effective. Comes with manual and band.", 1500, 3500, USED_ALL),
        new ListingTemplate("Bose SoundLink Mini II Bluetooth Speaker",
            "Excellent battery life, cradle and charger included.", 6000, 11000, USED_ALL),
        new ListingTemplate("Premium Suitcase Set (28\" + 24\" + 20\")",
            "Hard-shell, four spinner wheels, used for one international trip.", 6000, 13000, USED_OR_NEW),
        new ListingTemplate("Treadmill (Powermax Foldable 2HP)",
            "Used at home, working perfectly, with safety key and manual.", 14000, 25000, USED_ALL)
    };
}
