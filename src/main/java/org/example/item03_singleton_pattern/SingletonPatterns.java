package org.example.item03_singleton_pattern;
// ============================================================================
// Item 3: Enforce the singleton property with a private constructor or an enum type
// Effective Java, 3rd Edition - Joshua Bloch
// ============================================================================
// این فایل شامل Anti-Pattern ها و Best Practice ها برای Singleton است
// ============================================================================

import java.io.*;
import java.lang.reflect.Constructor;

public class SingletonPatterns {

    // ========================================================================
    // ❌ ANTI-PATTERN #1: Public Constructor
    // هر کس می‌تواند new کند — اصلاً Singleton نیست!
    // ========================================================================
    public static class BadConfig_AntiPattern1 {
        public BadConfig_AntiPattern1() {}  // constructor عمومی!

        private static BadConfig_AntiPattern1 instance;

        public static BadConfig_AntiPattern1 getInstance() {
            if (instance == null) {
                instance = new BadConfig_AntiPattern1();
            }
            return instance;
        }
    }
    // مشکل: constructor عمومی دارد — هر کس می‌تواند new BadConfig() بزند
    // و نمونه‌های مختلف بسازد. حتی اگر از getInstance استفاده کند،
    // باز هم Singleton بودن تضمین نیست.

    // ========================================================================
    // ❌ ANTI-PATTERN #2: Lazy Initialization بدون Thread Safety
    // Race Condition در محیط Multi-Thread
    // ========================================================================
    public static class UnsafeSingleton_AntiPattern2 {
        private static UnsafeSingleton_AntiPattern2 instance;  // بدون volatile!

        private UnsafeSingleton_AntiPattern2() {}

        public static UnsafeSingleton_AntiPattern2 getInstance() {
            if (instance == null) {           // Thread A اینجاست
                instance = new UnsafeSingleton_AntiPattern2();
                // Thread B هم اینجاست → دو نمونه!
            }
            return instance;
        }
    }
    // مشکل: در محیط Multi-Thread دو Thread ممکن است همزمان وارد if شوند
    // و دو نمونه بسازند. این یک Race Condition کلاسیک است.

    // ========================================================================
    // ❌ ANTI-PATTERN #3: Serializable بدون readResolve
    // Serialization نمونه جدید می‌سازد
    // ========================================================================
    public static class BrokenSerializableSingleton_AntiPattern3 implements Serializable {
        private static final long serialVersionUID = 1L;

        private static final BrokenSerializableSingleton_AntiPattern3 INSTANCE =
            new BrokenSerializableSingleton_AntiPattern3();

        private BrokenSerializableSingleton_AntiPattern3() {}

        public static BrokenSerializableSingleton_AntiPattern3 getInstance() {
            return INSTANCE;
        }
        // ❌ readResolve نداریم!
    }
    // مشکل: هر بار ObjectInputStream.readObject() صدا زده شود،
    // یک نمونه جدید ساخته می‌شود. Singleton شکسته می‌شود!

    // ========================================================================
    // ❌ ANTI-PATTERN #4: Cloneable — نمونه کپی می‌شود!
    // ========================================================================
    public static class CloneableSingleton_AntiPattern4 implements Cloneable {
        public static final CloneableSingleton_AntiPattern4 INSTANCE =
            new CloneableSingleton_AntiPattern4();

        private CloneableSingleton_AntiPattern4() {}

        @Override
        public CloneableSingleton_AntiPattern4 clone() {
            try {
                return (CloneableSingleton_AntiPattern4) super.clone();  // ❌ نمونه جدید!
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }
    // مشکل: clone() یک نمونه کاملاً جدید می‌سازد.
    // اگر کلاس Cloneable باشد و clone() را override نکنید
    // یا از super.clone() استفاده کنید، Singleton شکسته می‌شود.

    // ========================================================================
    // ✅ BEST PRACTICE #1: Enum Singleton — روش پیشنهادی Joshua Bloch
    // کوتاه، امن، Serializable-ready
    // ========================================================================
    public enum AppConfig_BestPractice1 {
        INSTANCE;

        private String databaseUrl = "jdbc:postgresql://localhost/db";
        private int maxConnections = 10;

        public String getDatabaseUrl() {
            return databaseUrl;
        }

        public void setDatabaseUrl(String url) {
            this.databaseUrl = url;
        }

        public int getMaxConnections() {
            return maxConnections;
        }
    }
    // چرا عالی است:
    // - JVM تضمین می‌کند فقط یک INSTANCE وجود دارد
    // - Serialization خودکار
    // - Reflection-proof
    // - Thread-safe
    // - نیازی به readResolve یا synchronized نیست

    // ========================================================================
    // ✅ BEST PRACTICE #2: Eager Initialization با public final field
    // ساده، واضح، Thread-safe
    // ========================================================================
    public static class EagerSingleton_BestPractice2 {
        public static final EagerSingleton_BestPractice2 INSTANCE =
            new EagerSingleton_BestPractice2();

        private EagerSingleton_BestPractice2() {
            // دفاع در برابر Reflection
            if (INSTANCE != null) {
                throw new IllegalStateException("Singleton already initialized!");
            }
        }

        public void doSomething() {
            System.out.println("Doing something...");
        }
    }
    // مزایا:
    // - API کاملاً واضح است که Singleton است
    // - final تضمین می‌کند reference تغییر نکند
    // - constructor از Reflection دفاع می‌کند
    // - JVM تضمین Thread-safety در initialization دارد

    // ========================================================================
    // ✅ BEST PRACTICE #3: Static Factory + Bill Pugh Singleton Holder
    // Lazy Initialization بدون synchronized
    // ========================================================================
    public static class LazySingleton_BestPractice3 {
        private LazySingleton_BestPractice3() {
            // دفاع در برابر Reflection
            if (SingletonHolder.INSTANCE != null) {
                throw new IllegalStateException("Singleton already initialized!");
            }
        }

        // کلاس داخلی static — فقط در اولین دسترسی بارگذاری می‌شود
        private static class SingletonHolder {
            private static final LazySingleton_BestPractice3 INSTANCE =
                new LazySingleton_BestPractice3();
        }

        public static LazySingleton_BestPractice3 getInstance() {
            return SingletonHolder.INSTANCE;
        }

        public void doSomething() {
            System.out.println("Lazy singleton doing something...");
        }
    }
    // Bill Pugh Pattern:
    // - کلاس داخلی static فقط زمانی بارگذاری می‌شود که getInstance() صدا زده شود
    // - JVM تضمین می‌کند بارگذاری کلاس Thread-safe است
    // - بدون نیاز به synchronized!

    // ========================================================================
    // ✅ BEST PRACTICE #4: Serializable Singleton با readResolve
    // اگر Enum نمی‌توانید استفاده کنید و Serializable لازم دارید
    // ========================================================================
    public static class SerializableSingleton_BestPractice4 implements Serializable {
        private static final long serialVersionUID = 1L;

        private static final SerializableSingleton_BestPractice4 INSTANCE =
            new SerializableSingleton_BestPractice4();

        private String configValue = "default";

        private SerializableSingleton_BestPractice4() {}

        public static SerializableSingleton_BestPractice4 getInstance() {
            return INSTANCE;
        }

        // ✅ نمونه جدید Deserialize شده را دور بریز
        private Object readResolve() {
            return INSTANCE;
        }

        // ✅ از Clone جلوگیری کنید
        @Override
        protected Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Singleton cannot be cloned");
        }

        public String getConfigValue() {
            return configValue;
        }

        public void setConfigValue(String value) {
            this.configValue = value;
        }
    }
    // نکات:
    // - readResolve نمونه جدید را دور می‌ریزد
    // - clone() را override و throw می‌کند
    // - فیلدها transient باشند (اگر state دارند)

    // ========================================================================
    // 🔓 DEMO: حمله Reflection به Singleton معمولی
    // ========================================================================
    public static void demonstrateReflectionAttack() {
        System.out.println("\n=== 🔓 حمله Reflection ===");
        try {
            Constructor<EagerSingleton_BestPractice2> ctor =
                EagerSingleton_BestPractice2.class.getDeclaredConstructor();
            ctor.setAccessible(true);  // دسترسی به private constructor!

            EagerSingleton_BestPractice2 fake = ctor.newInstance();
            System.out.println("fake == INSTANCE: " + (fake == EagerSingleton_BestPractice2.INSTANCE));
            // اگر constructor دفاع نکرده بود: false
            // اما چون دفاع کرده، IllegalStateException می‌دهد
        } catch (IllegalStateException e) {
            System.out.println("✅ Reflection attack blocked: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ========================================================================
    // 🔓 DEMO: حمله Serialization
    // ========================================================================
    public static void demonstrateSerializationAttack() throws Exception {
        System.out.println("\n=== 🔓 حمله Serialization ===");

        // Serialize
        SerializableSingleton_BestPractice4 original =
            SerializableSingleton_BestPractice4.getInstance();
        original.setConfigValue("modified");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        SerializableSingleton_BestPractice4 deserialized =
            (SerializableSingleton_BestPractice4) ois.readObject();

        System.out.println("deserialized == original: " + (deserialized == original));
        System.out.println("Same instance preserved: " + (deserialized == SerializableSingleton_BestPractice4.getInstance()));
        System.out.println("Config value preserved: " + deserialized.getConfigValue());
    }

    // ========================================================================
    // 🛡️ DEMO: Enum در برابر حملات
    // ========================================================================
    public static void demonstrateEnumSafety() {
        System.out.println("\n=== 🛡️ Enum Safety ===");

        // Reflection Attack
        try {
            Constructor<AppConfig_BestPractice1> ctor =
                AppConfig_BestPractice1.class.getDeclaredConstructor(String.class, int.class);
            ctor.setAccessible(true);
            AppConfig_BestPractice1 fake = ctor.newInstance("FAKE", 999);
        } catch (NoSuchMethodException e) {
            System.out.println("✅ Enum constructor not accessible via Reflection");
        } catch (Exception e) {
            System.out.println("✅ Reflection blocked: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // Serialization is automatic and safe for Enum
        System.out.println("✅ Enum Serialization is automatically safe");

        // Clone is not possible for Enum
//        try {
//            AppConfig_BestPractice1.INSTANCE.clone();
//        } catch (CloneNotSupportedException e) {
//            System.out.println("✅ Enum cannot be cloned: " + e.getMessage());
//        }
    }

    // ========================================================================
    // MAIN METHOD — اجرای تمام Demo ها
    // ========================================================================
    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("  Effective Java - Item 3: Singleton Patterns");
        System.out.println("  Anti-Patterns vs Best Practices");
        System.out.println("==================================================");

        // استفاده از Enum Singleton
        System.out.println("\n=== ✅ Enum Singleton Usage ===");
        AppConfig_BestPractice1 config = AppConfig_BestPractice1.INSTANCE;
        System.out.println("Database URL: " + config.getDatabaseUrl());
        config.setDatabaseUrl("jdbc:postgresql://prod-server/db");
        System.out.println("Updated URL: " + AppConfig_BestPractice1.INSTANCE.getDatabaseUrl());

        // استفاده از Eager Singleton
        System.out.println("\n=== ✅ Eager Singleton Usage ===");
        EagerSingleton_BestPractice2 eager = EagerSingleton_BestPractice2.INSTANCE;
        eager.doSomething();

        // استفاده از Lazy Singleton
        System.out.println("\n=== ✅ Lazy Singleton (Bill Pugh) Usage ===");
        LazySingleton_BestPractice3 lazy = LazySingleton_BestPractice3.getInstance();
        lazy.doSomething();

        // استفاده از Serializable Singleton
        System.out.println("\n=== ✅ Serializable Singleton Usage ===");
        SerializableSingleton_BestPractice4 serializable =
            SerializableSingleton_BestPractice4.getInstance();
        System.out.println("Config: " + serializable.getConfigValue());

        // حملات
        demonstrateReflectionAttack();
        demonstrateSerializationAttack();
        demonstrateEnumSafety();

        System.out.println("\n==================================================");
        System.out.println("  نتیجه‌گیری:");
        System.out.println("  1. Enum Singleton بهترین روش است");
        System.out.println("  2. Eager Initialization ساده و واضح است");
        System.out.println("  3. Bill Pugh برای Lazy Initialization عالی است");
        System.out.println("  4. readResolve برای Serializable Singleton ضروری است");
        System.out.println("==================================================");
    }
}