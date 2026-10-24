<div dir="rtl">

<a id="top"></a>

# آیتم ۴۴: رابط‌های تابعی استاندارد را ترجیح دهید

## (Favor the use of standard functional interfaces)

موضوع این آیتم یکی از مهم‌ترین اصول طراحی API در جاوا است: وقتی می‌خواهیم متدی بنویسیم که یک رفتار را به‌عنوان ورودی دریافت کند، معمولاً بهتر است از Functional Interfaceهای استاندارد جاوا استفاده کنیم، نه اینکه برای هر نیاز یک رابط تابعی جدید بسازیم.

این آیتم ادامه طبیعی Item 42 و Item 43 است. در Item 42 یاد گرفتیم از Lambda به‌جای Anonymous Class استفاده کنیم و در Item 43 یاد گرفتیم چه زمانی Method Reference خوانایی بیشتری دارد. حالا در Item 44 می‌خواهیم یاد بگیریم APIهای خودمان را چطور طراحی کنیم تا استفاده از Lambdaها ساده‌تر، استانداردتر و قابل‌فهم‌تر باشد.

---

## فهرست مطالب

- [۱. چرا با ورود Lambdaها، طراحی API تغییر کرده است؟](#why-changed)
- [۲. مثال اصلی کتاب: استفاده از LinkedHashMap برای ساخت Cache](#linkedhashmap-example)
  - [چرا طراحی این تابع کمی پیچیده است؟](#why-complex)
- [۳. اصل کلیدی Item 44: چرخ را دوباره اختراع نکنید!](#dont-reinvent)
- [۴. شش Functional Interface اصلی که باید بشناسیم](#six-main)
  - [چطور این شش رابط را به خاطر بسپاریم؟](#how-to-remember)
- [۵. انواع تخصصی برای نوع‌های primitive](#primitive-types)
  - [خانواده‌های مهم رابط‌های primitive](#primitive-families)
  - [انواع دوورودی](#two-argument-types)
- [۶. چه زمانی باید Functional Interface اختصاصی خودمان را بسازیم؟](#when-custom)
  - [حالت اول: هیچ رابط استانداردی نیاز ما را برآورده نمی‌کند](#no-standard)
  - [حالت دوم: رابط اختصاصی معنای مهمی دارد](#meaningful-custom)
  - [سه معیار برای ساخت رابط اختصاصی](#three-criteria)
- [۷. چرا باید از `@FunctionalInterface` استفاده کنیم؟](#functionalinterface-annotation)
- [۸. یک اشتباه مهم در طراحی API: Overloadهای مبهم](#ambiguous-overloads)
  - [توصیه کتاب چیست؟](#recommendation)
- [۹. جمع‌بندی نهایی Item 44](#final-summary)

[بازگشت به بالا](#top)

---

<a id="why-changed"></a>
## ۱. چرا با ورود Lambdaها، طراحی API تغییر کرده است؟

قبل از Java 8، برای تغییر رفتار یک کلاس معمولاً از وراثت و الگوهایی مانند Template Method استفاده می‌کردیم.

اما با معرفی Lambdaها، می‌توانیم به‌جای تعریف کلاس فرزند، رفتار موردنظر را مستقیماً به سازنده یا متد پاس بدهیم:

<div dir="ltr">

```java
employees.removeIf(employee -> employee.isInactive());
```
</div>

این روش مزایای مهمی دارد:

| مزیت | توضیح |
|------|-------|
| نیاز به تعریف کلاس‌های اضافی کمتر می‌شود | رفتار به‌صورت تابع پاس داده می‌شود |
| رفتار موردنظر به‌صورت مستقیم مشخص می‌شود | کد خواناتر است |
| API انعطاف‌پذیرتر می‌شود | رفتار قابل تغییر است بدون وراثت |
| استفاده از Lambda و Method Reference آسان‌تر می‌شود | کد کوتاه‌تر |

> **نتیجه:** در طراحی APIهای مدرن جاوا، باید به‌جای اینکه همیشه رفتار را از طریق وراثت قابل‌تغییر کنیم، امکان ارسال رفتار به‌صورت تابع را نیز در نظر بگیریم.

[بازگشت به بالا](#top)

---

<a id="linkedhashmap-example"></a>
## ۲. مثال اصلی کتاب: استفاده از LinkedHashMap برای ساخت Cache

فرض کنید می‌خواهیم یک Cache داشته باشیم که حداکثر ۱۰۰ ورودی نگه دارد:

<div dir="ltr">

```java
@Override
protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() > 100;
}
```
</div>

- اگر `true` برگرداند، قدیمی‌ترین ورودی حذف می‌شود
- اگر `false` برگرداند، آن ورودی باقی می‌ماند

این روش کار می‌کند، اما مشکل طراحی آن این است که برای تغییر رفتار باید متد را Override کنیم؛ یعنی رفتار موردنظر به سازوکار وراثت وابسته است.

> **کتاب می‌گوید:** اگر `LinkedHashMap` امروزه از ابتدا طراحی می‌شد، بهتر بود سازنده یا Factory Method آن یک تابع دریافت می‌کرد تا کاربر بتواند قانون حذف ورودی‌های قدیمی را مستقیماً مشخص کند.

<a id="why-complex"></a>
### چرا طراحی این تابع کمی پیچیده است؟

در نگاه اول، ممکن است فکر کنیم تابع موردنظر فقط باید یک ورودی از نوع `Map.Entry<K, V>` دریافت کند و یک `boolean` برگرداند.

اما متد `removeEldestEntry` از `size()` نیز استفاده می‌کند. این متد به وضعیت خود Map وابسته است. اگر تابع را هنگام ساخت Map به سازنده بدهیم، خود Map هنوز ساخته نشده است.

**راه‌حل:** تابع هم خود Map و هم قدیمی‌ترین ورودی را به‌عنوان ورودی دریافت کند:

<div dir="ltr">

```java
@FunctionalInterface
interface EldestEntryRemovalFunction<K, V> {
    boolean remove(Map<K, V> map, Map.Entry<K, V> eldest);
}
```
</div>

> **اما آیا لازم است آن را تعریف کنیم؟ خیر؛ چون جاوا از قبل یک رابط استاندارد دارد که همین نیاز را برطرف می‌کند.**

[بازگشت به بالا](#top)

---

<a id="dont-reinvent"></a>
## ۳. اصل کلیدی Item 44: چرخ را دوباره اختراع نکنید!

در بسته `java.util.function` مجموعه‌ای از Functional Interfaceهای استاندارد وجود دارد:

<div dir="ltr">

```java
BiPredicate<Map<K, V>, Map.Entry<K, V>> shouldRemove;
```
</div>

از نظر ساختاری، این رابط با `EldestEntryRemovalFunction` ما سازگار است:

<div dir="ltr">

```java
BiPredicate<Map<K, V>, Map.Entry<K, V>> shouldRemove =
    (map, eldest) -> map.size() > 100;
```
</div>

### چرا استفاده از `BiPredicate` بهتر است؟

| مزیت | توضیح |
|------|-------|
| **یادگیری آسان‌تر** | توسعه‌دهندگان جاوا از قبل با رابط‌های استاندارد آشنا هستند |
| **کاهش پیچیدگی مفهومی** | لازم نیست هر بار با یک نام و قرارداد جدید آشنا شویم |
| **سازگاری بیشتر** | کدهای مختلف می‌توانند از یک نوع تابع مشترک استفاده کنند |
| **متدهای پیش‌فرض آماده** | بعضی از این رابط‌ها امکانات مفیدی برای ترکیب و تبدیل رفتارها دارند |

<div dir="ltr">

```java
Predicate<String> notEmpty = s -> !s.isEmpty();
Predicate<String> startsWithA = s -> s.startsWith("A");

Predicate<String> valid = notEmpty.and(startsWithA);
```
</div>

> **قاعده عملی:** اگر یکی از Functional Interfaceهای استاندارد جاوا نیاز شما را برطرف می‌کند، معمولاً همان را انتخاب کنید.

[بازگشت به بالا](#top)

---

<a id="six-main"></a>
## ۴. شش Functional Interface اصلی که باید بشناسیم

در `java.util.function` تعداد زیادی رابط تابعی وجود دارد. حفظ کردن همه آن‌ها ضروری نیست؛ کافی است شش رابط اصلی را بشناسیم.

| رابط | تعداد ورودی | نوع خروجی | کاربرد متداول | امضا |
|------|-------------|-----------|----------------|-------|
| `UnaryOperator<T>` | ۱ | همان نوع ورودی | تغییر یک مقدار | `T apply(T t)` |
| `BinaryOperator<T>` | ۲ | همان نوع ورودی | ترکیب دو مقدار | `T apply(T t1, T t2)` |
| `Predicate<T>` | ۱ | `boolean` | بررسی شرط | `boolean test(T t)` |
| `Function<T,R>` | ۱ | نوع R | تبدیل یک مقدار | `R apply(T t)` |
| `Supplier<T>` | ۰ | نوع T | تولید یا تأمین مقدار | `T get()` |
| `Consumer<T>` | ۱ | `void` | مصرف یا پردازش مقدار | `void accept(T t)` |

<div dir="ltr">

```java
UnaryOperator<String> lowercase = String::toLowerCase;         // "JAVA" → "java"
BinaryOperator<BigInteger> add = BigInteger::add;
Predicate<String> isEmpty = String::isEmpty;
Function<String, Integer> length = String::length;             // "Java" → 4
Supplier<Instant> clock = Instant::now;
Consumer<String> printer = System.out::println;
```
</div>

<a id="how-to-remember"></a>
### چطور این شش رابط را به خاطر بسپاریم؟

> **تفاوت مهم:** `Function<T, R>` نوع ورودی و خروجی می‌توانند متفاوت باشند، اما در `UnaryOperator<T>` هر دو از نوع `T` هستند.

> **نکته:** نام `Supplier` به این معنا نیست که حتماً باید یک شیء جدید بسازد؛ فقط یعنی هنگام فراخوانی، مقداری را ارائه می‌کند.

[بازگشت به بالا](#top)

---

<a id="primitive-types"></a>
## ۵. انواع تخصصی برای نوع‌های primitive

یکی از نکات مهم کتاب، پرهیز از استفاده غیرضروری از Wrapperها مانند `Integer`، `Long` و `Double` است.

<div dir="ltr">

```java
Predicate<Integer> isPositive = number -> number > 0;   // ⚠️ Boxing
IntPredicate isPositive = number -> number > 0;         // ✅ Primitive
```
</div>

| تفاوت | `Predicate<Integer>` | `IntPredicate` |
|--------|---------------------|----------------|
| نوع | مرجع (`Integer`) | Primitive (`int`) |
| Boxing | ✅ دارد | ❌ ندارد |
| عملکرد در پردازش زیاد | سربار | بهتر |

<a id="primitive-families"></a>
### خانواده‌های مهم رابط‌های primitive

جاوا برای `int`، `long` و `double` نسخه‌های تخصصی بسیاری ارائه می‌کند:

| رابط | کاربرد |
|------|--------|
| `IntPredicate` | دریافت `int` و برگرداندن `boolean` |
| `LongPredicate` | دریافت `long` و برگرداندن `boolean` |
| `DoublePredicate` | دریافت `double` و برگرداندن `boolean` |
| `IntConsumer` | دریافت `int` بدون خروجی |
| `LongConsumer` | دریافت `long` بدون خروجی |
| `DoubleConsumer` | دریافت `double` بدون خروجی |
| `IntSupplier` | تولید یک `int` |
| `LongSupplier` | تولید یک `long` |
| `DoubleSupplier` | تولید یک `double` |
| `IntUnaryOperator` | دریافت و برگرداندن `int` |
| `LongUnaryOperator` | دریافت و برگرداندن `long` |
| `DoubleUnaryOperator` | دریافت و برگرداندن `double` |
| `IntBinaryOperator` | دریافت دو `int` و برگرداندن `int` |
| `LongBinaryOperator` | دریافت دو `long` و برگرداندن `long` |
| `DoubleBinaryOperator` | دریافت دو `double` و برگرداندن `double` |

**تبدیل بین انواع مختلف:**

<div dir="ltr">

```java
LongToIntFunction convert = value -> (int) value;

LongFunction<int[]> createArray = length -> new int[(int) length];
```
</div>

> **توجه:** تبدیل `long` به `int` در این مثال ممکن است در صورت بزرگ بودن مقدار، اطلاعات را از بین ببرد.

<a id="two-argument-types"></a>
### انواع دوورودی

| رابط | ورودی | خروجی |
|------|-------|--------|
| `BiPredicate<T, U>` | دو | `boolean` |
| `BiFunction<T, U, R>` | دو | نوع `R` |
| `BiConsumer<T, U>` | دو | `void` |
| `ToIntBiFunction<T, U>` | دو | `int` |
| `ToLongBiFunction<T, U>` | دو | `long` |

<div dir="ltr">

```java
BiPredicate<String, Integer> hasLength =
    (text, length) -> text.length() == length;
```
</div>

> **جمع‌بندی کتاب:** در مجموع ۴۳ رابط تابعی استاندارد بررسی می‌شود. لازم نیست همه آن‌ها را حفظ کنید؛ الگوی نام‌گذاری و شناخت شش رابط اصلی برای انتخاب درست در بیشتر موارد کافی است.

[بازگشت به بالا](#top)

---

<a id="when-custom"></a>
## ۶. چه زمانی باید Functional Interface اختصاصی خودمان را بسازیم؟

<a id="no-standard"></a>
### حالت اول: هیچ رابط استانداردی نیاز ما را برآورده نمی‌کند

<div dir="ltr">

```java
@FunctionalInterface
interface ThreeArgumentPredicate<A, B, C> {
    boolean test(A a, B b, C c);
}
```
</div>

یا زمانی که لازم باشد متد تابعی یک **Checked Exception** پرتاب کند:

<div dir="ltr">

```java
@FunctionalInterface
interface CheckedFunction<T, R> {
    R apply(T value) throws IOException;
}
```
</div>

<a id="meaningful-custom"></a>
### حالت دوم: رابط اختصاصی معنای مهمی دارد

مثال مهم کتاب، `Comparator<T>` است:

<div dir="ltr">

```java
Comparator<Employee> comparator =
    Comparator.comparing(Employee::getLastName)
              .thenComparing(Employee::getFirstName);
```
</div>

**سه دلیل اصلی برای `Comparator` اختصاصی:**

| دلیل | توضیح |
|------|-------|
| **۱. نام توصیفی و معنادار** | `Comparator<Employee>` بلافاصله هدف را می‌رساند؛ `ToIntBiFunction<Employee, Employee>` این معنا را منتقل نمی‌کند |
| **۲. قرارداد مشخص** | سازگاری نتایج مقایسه و رعایت قواعد ترتیب |
| **۳. متدهای پیش‌فرض کاربردی** | `thenComparing` و سایر متدهای ترکیب |

<a id="three-criteria"></a>
### سه معیار برای ساخت رابط اختصاصی

| معیار | توضیح |
|--------|-------|
| **۱** | رابط قرار است بسیار رایج باشد و نام معنادار آن ارزش زیادی داشته باشد |
| **۲** | قرارداد رفتاری خاص و مهمی دارد |
| **۳** | به متدهای پیش‌فرض اختصاصی برای تبدیل یا ترکیب رفتارها نیاز دارد |

> **در غیر این صورت، استفاده از رابط استاندارد معمولاً انتخاب ساده‌تر و مناسب‌تری است.**

[بازگشت به بالا](#top)

---

<a id="functionalinterface-annotation"></a>
## ۷. چرا باید از `@FunctionalInterface` استفاده کنیم؟

<div dir="ltr">

```java
@FunctionalInterface
interface StringProcessor {
    String process(String input);
}
```
</div>

**سه مزیت اصلی:**

| مزیت | توضیح |
|------|-------|
| **۱. اعلام قصد طراح** | به خواننده و ابزارهای توسعه می‌گوید که این رابط برای استفاده با Lambda طراحی شده است |
| **۲. بررسی توسط کامپایلر** | اگر رابط شرایط لازم برای Functional Interface بودن را نداشته باشد، کامپایلر خطا می‌دهد |
| **۳. جلوگیری از تغییرات ناخواسته** | اگر کسی بعداً یک متد abstract دیگر به رابط اضافه کند، کامپایلر مشکل را تشخیص می‌دهد |

> **نکته مهم:** Functional Interface باید دقیقاً یک متد abstract داشته باشد. وجود متدهای `default` و `static` مانعی ایجاد نمی‌کند؛ همچنین متدهایی که با امضای متدهای عمومی `Object` هم‌خوانی دارند، در شمارش متد abstract مؤثر نیستند.

> **`@FunctionalInterface` صرفاً یک توضیح مستنداتی نیست؛ کامپایلر نیز آن را بررسی می‌کند.**

[بازگشت به بالا](#top)

---

<a id="ambiguous-overloads"></a>
## ۸. یک اشتباه مهم در طراحی API: Overloadهای مبهم

آخرین نکته مهم Item 44 به متدهایی مربوط است که چند نسخه Overload شده دارند و هر نسخه یک Functional Interface متفاوت را در همان موقعیت پارامتر می‌پذیرد.

<div dir="ltr">

```java
executor.submit(() -> {
    System.out.println("Working...");
    return null;
});
```
</div>

این Lambda یک مقدار برمی‌گرداند؛ بنابراین با `Callable` سازگار است، نه با `Runnable`.

اما اگر Lambda را طوری بنویسیم که هم با `Runnable` و هم با `Callable` سازگار باشد، ممکن است کامپایلر نتواند نسخه مناسب را به‌طور یکتا انتخاب کند:

<div dir="ltr">

```java
executor.submit(() -> System.out.println("Working..."));
```
</div>

در چنین شرایطی گاهی باید نوع را به‌صورت صریح مشخص کنیم:

<div dir="ltr">

```java
executor.submit((Runnable)
    () -> System.out.println("Working..."));
```
</div>

<a id="recommendation"></a>
### توصیه کتاب چیست؟

> **از تعریف Overloadهایی که Functional Interfaceهای متفاوتی را در یک موقعیت پارامتر می‌پذیرند، زمانی که ممکن است باعث ابهام برای فراخواننده شوند، خودداری کنید.**

این توصیه به Item 52، یعنی استفاده سنجیده از Overloading، مرتبط است.

> **نکته:** مسئله این نیست که هر Overload با Functional Interface اشتباه است؛ طراحی API نباید توسعه‌دهنده را مجبور کند برای رفع ابهام‌های قابل‌اجتناب، مرتب Cast بنویسد.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۹. جمع‌بندی نهایی Item 44

| اصل | توضیح |
|-----|-------|
| **از Functional Interfaceهای استاندارد شروع کنید** | اگر `Predicate`، `Function`، `Consumer` یا یکی از انواع استاندارد نیاز شما را برآورده می‌کند، نیازی به تعریف رابط جدید ندارید |
| **انواع primitive را جدی بگیرید** | در پردازش‌های پرتعداد، رابط‌هایی مانند `IntPredicate` و `IntUnaryOperator` می‌توانند از سربار Boxing و Unboxing جلوگیری کنند |
| **وقتی معنا یا قرارداد اهمیت دارد، رابط اختصاصی بسازید** | نام‌هایی مانند `Comparator` اطلاعات بیشتری درباره هدف و قرارداد رابط منتقل می‌کنند |
| **روی Functional Interfaceهای اختصاصی از `@FunctionalInterface` استفاده کنید** | این کار قصد طراحی را روشن می‌کند و از تغییرات ناسازگار ناخواسته جلوگیری می‌کند |
| **از Overloadهای مبهم پرهیز کنید** | طراحی API باید استفاده از Lambdaها را ساده کند، نه اینکه توسعه‌دهنده را مجبور به Castهای غیرضروری کند |

### جدول انتخاب سریع

| نیاز | انتخاب |
|------|--------|
| بررسی شرط تک‌ورودی | `Predicate<T>` |
| بررسی شرط دوورودی | `BiPredicate<T, U>` |
| تبدیل نوع | `Function<T, R>` |
| تولید مقدار | `Supplier<T>` |
| مصرف مقدار | `Consumer<T>` |
| تغییر مقدار | `UnaryOperator<T>` |
| ترکیب دو مقدار | `BinaryOperator<T>` |
| پردازش primitive | `IntPredicate`، `IntUnaryOperator` و ... |
| مقایسه معنادار | `Comparator<T>` |
| نیاز به Checked Exception | رابط اختصاصی |

### نکته پایانی

> **مهم‌ترین پیام این آیتم این است که طراحی API با ورود Lambdaها فقط به معنای پذیرش تابع به‌عنوان ورودی نیست؛ بلکه باید نوع تابع را هم هوشمندانه انتخاب کنیم. رابط استاندارد مناسب، یادگیری و استفاده از API را آسان‌تر می‌کند، درحالی‌که رابط اختصاصی باید زمانی انتخاب شود که نام، قرارداد یا قابلیت‌های ویژه آن واقعاً ارزش افزوده‌ای ایجاد کند.**

---

[بازگشت به بالا](#top)

</div>