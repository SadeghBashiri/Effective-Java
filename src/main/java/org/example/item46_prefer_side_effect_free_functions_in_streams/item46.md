<div dir="rtl">

<a id="top"></a>

# آیتم ۴۶: توابع بدون عارضه جانبی را در Streamها ترجیح دهید

## (Prefer side-effect-free functions in streams)

> **پیام اصلی این آیتم:** در Stream Pipelineها، به‌جای تغییر دادن وضعیت متغیرهای بیرونی، از توابعی استفاده کنید که نتیجه را از طریق خود Pipeline تولید و منتقل می‌کنند.

این آیتم ادامه مستقیم Item 45 است. در آیتم قبلی یاد گرفتیم که نباید همه حلقه‌ها را بی‌دلیل به Stream تبدیل کنیم. حالا Joshua Bloch یک قدم جلوتر می‌رود و توضیح می‌دهد که اگر تصمیم گرفتیم از Stream استفاده کنیم، باید الگوی فکری آن را هم رعایت کنیم؛ نه اینکه فقط ظاهر کد را با Lambda و Method Reference تغییر دهیم.

---

## فهرست مطالب

- [۱. چرا Stream فقط یک API نیست، بلکه یک سبک برنامه‌نویسی است؟](#stream-style)
- [۲. Side Effect چیست؟](#side-effect)
  - [Pure Function دقیقاً چه معنایی دارد؟](#pure-function)
- [۳. مثال اول کتاب: استفاده نادرست از Stream برای شمارش کلمات](#bad-example)
  - [چرا کتاب این کار را توصیه نمی‌کند؟](#why-not-recommended)
- [۴. نسخه صحیح: استفاده از Collector به‌جای تغییر وضعیت بیرونی](#correct-version)
  - [چرا این نسخه با الگوی Stream سازگارتر است؟](#why-better)
- [۵. چرا `forEach` یکی از کم‌توان‌ترین عملیات‌های نهایی برای محاسبات است؟](#foreach-weakness)
- [۶. Collector چیست و چرا باید آن را یاد بگیریم؟](#collector)
  - [سه Collector برای جمع‌آوری عناصر در Collection](#collection-collectors)
- [۷. استخراج ده کلمه پرتکرار از جدول فراوانی](#top-ten)
- [۸. آشنایی با `toMap`: تبدیل Stream به Map](#tomap)
  - [مشکل کلیدهای تکراری](#duplicate-keys)
  - [نسخه سه‌پارامتری `toMap`](#three-param-tomap)
  - [مثال کتاب: انتخاب پرفروش‌ترین آلبوم هر هنرمند](#top-album)
  - [سیاست «آخرین مقدار برنده است»](#last-write-wins)
- [۹. `groupingBy`: گروه‌بندی عناصر بر اساس یک ویژگی](#groupingby)
  - [تغییر نوع مقادیر گروه‌ها با Downstream Collector](#downstream)
  - [تعیین نوع Map اصلی](#map-type)
  - [تفاوت `groupingBy` و `partitioningBy`](#partitioningby)
- [۱۰. آیا باید تمام متدهای `Collectors` را یاد بگیریم؟](#all-collectors)
- [۱۱. سه Collector دیگر که ارزش شناختن دارند](#other-collectors)
  - [الف) `minBy` و `maxBy`](#minby-maxby)
  - [ب) `joining`](#joining)
- [۱۲. یک اشتباه رایج: تصور اینکه هر Lambda بدون Side Effect است](#common-mistake)
- [۱۳. جمع‌بندی نهایی Item 46](#final-summary)

[بازگشت به بالا](#top)

---

<a id="stream-style"></a>
## ۱. چرا Stream فقط یک API نیست، بلکه یک سبک برنامه‌نویسی است؟

فرض کنید می‌خواهیم تعداد تکرار هر کلمه در یک فایل متنی را محاسبه کنیم:

```
Java is great
Java is powerful
Java is great
```

| کلمه | تعداد |
|-------|-------|
| `java` | 3 |
| `is` | 3 |
| `great` | 2 |
| `powerful` | 1 |

```
متن فایل → داده‌های اولیه → تبدیل کلمات به حروف کوچک (Normalization)
    → گروه‌بندی و شمارش (Reduction / Collection) → جدول فراوانی کلمات
```

> **ایده اصلی:** پردازش را به مجموعه‌ای از تبدیل‌ها تقسیم کنیم؛ به‌طوری که خروجی هر مرحله ورودی مرحله بعد باشد.

[بازگشت به بالا](#top)

---

<a id="side-effect"></a>
## ۲. Side Effect چیست؟

یک تابع زمانی دارای عارضه جانبی است که علاوه بر محاسبه نتیجه خود، وضعیت یا چیزی خارج از نتیجه محاسبه را نیز تغییر دهد.

**نمونه‌های متداول Side Effect:**
- تغییر دادن یک متغیر یا فیلد بیرونی
- اضافه یا حذف کردن عناصر یک Collection مشترک
- نوشتن در فایل یا پایگاه داده
- چاپ کردن خروجی
- تغییر وضعیت یک شیء که بخش‌های دیگر برنامه نیز به آن دسترسی دارند

<div dir="ltr">

```java
int square(int x) {
    return x * x;  // ✅ Pure
}
```
</div>

<div dir="ltr">

```java
int total = 0;

void addToTotal(int value) {
    total += value;  // ❌ Side Effect
}
```
</div>

> **نکته:** Side Effect همیشه بد نیست. برنامه‌ها برای ذخیره‌سازی، ارسال درخواست یا چاپ خروجی به آن نیاز دارند. نکته کتاب این است که توابعی که به عملیات Stream می‌دهیم، تا حد امکان باید بدون عارضه جانبی باشند.

<a id="pure-function"></a>
### Pure Function دقیقاً چه معنایی دارد؟

| ویژگی | توضیح |
|-------|-------|
| **۱** | نتیجه آن فقط به ورودی‌هایش وابسته است |
| **۲** | اجرای آن هیچ وضعیت بیرونی را تغییر نمی‌دهد |

> **نکته ظریف:** صرف استفاده از Lambda به این معنی نیست که تابع Pure است. Lambda هم می‌تواند متغیرهای بیرونی را تغییر دهد.

[بازگشت به بالا](#top)

---

<a id="bad-example"></a>
## ۳. مثال اول کتاب: استفاده نادرست از Stream برای شمارش کلمات

<div dir="ltr">

```java
Map<String, Long> freq = new HashMap<>();

try (Stream<String> words = new Scanner(file).tokens()) {
    words.forEach(word -> {
        freq.merge(word.toLowerCase(), 1L, Long::sum);
    });
}
```
</div>

**مشکل:** تمام کار محاسباتی داخل `forEach` انجام می‌شود و Lambda وضعیت یک متغیر بیرونی را تغییر می‌دهد.

```
words.forEach(word -> {
    // تغییر Map بیرونی
});
```

> یعنی ما فقط ظاهر حلقه را عوض کرده‌ایم؛ به‌جای `for` از `forEach` استفاده کرده‌ایم، اما همچنان همان الگوی تغییر دستی وضعیت را داریم.

<a id="why-not-recommended"></a>
### چرا کتاب این کار را توصیه نمی‌کند؟

| # | دلیل |
|---|-------|
| **۱** | از قابلیت‌های اصلی Stream استفاده نکرده‌ایم |
| **۲** | خوانایی کمتر است |
| **۳** | ترکیب عملیات دشوارتر می‌شود |
| **۴** | اجرای موازی می‌تواند مشکل‌ساز شود (`HashMap` برای تغییرات هم‌زمانی طراحی نشده است) |

[بازگشت به بالا](#top)

---

<a id="correct-version"></a>
## ۴. نسخه صحیح: استفاده از Collector به‌جای تغییر وضعیت بیرونی

<div dir="ltr">

```java
Map<String, Long> freq;

try (Stream<String> words = new Scanner(file).tokens()) {
    freq = words.collect(
        Collectors.groupingBy(
            String::toLowerCase,
            Collectors.counting()
        )
    );
}
```
</div>

| بخش | نقش |
|------|-----|
| `String::toLowerCase` | معیار گروه‌بندی |
| `Collectors.counting()` | محاسبه تعداد عناصر هر گروه |

> **معنای کلی:** کلمات را بر اساس شکل حروف کوچک‌شده آن‌ها گروه‌بندی کن و تعداد کلمات هر گروه را به‌عنوان مقدار نگه دار.

<a id="why-better"></a>
### چرا این نسخه با الگوی Stream سازگارتر است؟

| رویکرد | توضیح |
|--------|-------|
| **قبلی** | وضعیت یک Map را مرحله‌به‌مرحله تغییر می‌دهیم |
| **جدید** | مشخص می‌کنیم چه نتیجه‌ای می‌خواهیم؛ Collector مدیریت انباشتن را بر عهده دارد |

> **نکته درباره Locale:** در برنامه‌هایی که باید رفتار یکسانی در Localeهای مختلف داشته باشند، بهتر است `toLowerCase(Locale.ROOT)` را صریح مشخص کنیم.

[بازگشت به بالا](#top)

---

<a id="foreach-weakness"></a>
## ۵. چرا `forEach` یکی از کم‌توان‌ترین عملیات‌های نهایی برای محاسبات است؟

**الگوی اول (نامناسب):**

<div dir="ltr">

```java
Map<String, Long> freq = new HashMap<>();
words.forEach(word -> freq.merge(word.toLowerCase(), 1L, Long::sum));
```
</div>

**الگوی دوم (مناسب):**

<div dir="ltr">

```java
Map<String, Long> freq = words.collect(
    Collectors.groupingBy(String::toLowerCase, Collectors.counting())
);
```
</div>

> **توصیه کتاب:** `forEach` را عمدتاً برای گزارش دادن یا ارائه نتیجه نهایی محاسبه به کار ببریم.

<div dir="ltr">

```java
words.stream()
    .filter(word -> word.length() > 5)
    .forEach(System.out::println);
```
</div>

> **نکته:** `forEach` به‌خودی‌خود تضمین نمی‌کند که عملیات موازی به شکل امن یا با ترتیب مشخص انجام شوند.

[بازگشت به بالا](#top)

---

<a id="collector"></a>
## ۶. Collector چیست و چرا باید آن را یاد بگیریم؟

`Collector` راهبردی برای ترکیب عناصر Stream و تولید یک نتیجه نهایی است.

<div dir="ltr">

```java
List<String> names = words.stream()
    .filter(word -> word.length() > 3)
    .collect(Collectors.toList());

Set<String> uniqueWords = words.stream()
    .collect(Collectors.toSet());
```
</div>

<a id="collection-collectors"></a>
### سه Collector برای جمع‌آوری عناصر در Collection

| Collector | نتیجه |
|-----------|-------|
| `toList()` | یک List |
| `toSet()` | یک Set |
| `toCollection(factory)` | نوع Collection انتخاب‌شده توسط برنامه‌نویس |

<div dir="ltr">

```java
TreeSet<String> sortedWords = words.stream()
    .collect(Collectors.toCollection(TreeSet::new));
```
</div>

> **نکته:** `Stream.toList()` با `Collectors.toList()` از نظر قرارداد تغییرپذیری نتیجه یکسان نیست؛ اگر به Collection تغییرپذیر نیاز دارید، انتخاب Collector مناسب اهمیت دارد.

[بازگشت به بالا](#top)

---

<a id="top-ten"></a>
## ۷. استخراج ده کلمه پرتکرار از جدول فراوانی

<div dir="ltr">

```java
List<String> topTen = freq.keySet().stream()
    .sorted(Comparator.comparing(freq::get).reversed())
    .limit(10)
    .collect(Collectors.toList());
```
</div>

| مرحله | عملیات |
|--------|--------|
| **۱** | `freq.keySet().stream()` — دریافت کلمات |
| **۲** | `Comparator.comparing(freq::get).reversed()` — مرتب‌سازی بر اساس تعداد تکرار (نزولی) |
| **۳** | `.limit(10)` — انتخاب ده کلمه اول |
| **۴** | `.collect(Collectors.toList())` — جمع‌آوری در List |

**با static import:**

<div dir="ltr">

```java
import static java.util.stream.Collectors.*;
import static java.util.Comparator.comparing;

List<String> topTen = freq.keySet().stream()
    .sorted(comparing(freq::get).reversed())
    .limit(10)
    .collect(toList());
```
</div>

> **نکته:** اگر چند کلمه تعداد تکرار یکسانی داشته باشند، ترتیب آن‌ها در این کد به‌تنهایی معیار ثانویه مشخصی ندارد.

[بازگشت به بالا](#top)

---

<a id="tomap"></a>
## ۸. آشنایی با `toMap`: تبدیل Stream به Map

<div dir="ltr">

```java
Collectors.toMap(keyMapper, valueMapper)
```
</div>

<div dir="ltr">

```java
Map<String, Operation> stringToEnum =
    Stream.of(Operation.values())
        .collect(Collectors.toMap(
            Object::toString,
            operation -> operation
        ));
```
</div>

<a id="duplicate-keys"></a>
### مشکل کلیدهای تکراری

اگر دو عنصر کلید یکسانی تولید کنند، عملیات معمولاً با `IllegalStateException` شکست می‌خورد.

<a id="three-param-tomap"></a>
### نسخه سه‌پارامتری `toMap`

<div dir="ltr">

```java
Collectors.toMap(keyMapper, valueMapper, mergeFunction)
```
</div>

<div dir="ltr">

```java
Map<String, Integer> totals = items.stream()
    .collect(Collectors.toMap(
        Item::category,
        Item::price,
        Integer::sum
    ));
```
</div>

<a id="top-album"></a>
### مثال کتاب: انتخاب پرفروش‌ترین آلبوم هر هنرمند

<div dir="ltr">

```java
Map<Artist, Album> topHits = albums.stream()
    .collect(Collectors.toMap(
        Album::artist,
        album -> album,
        BinaryOperator.maxBy(
            Comparator.comparing(Album::sales)
        )
    ));
```
</div>

| بخش | نقش |
|------|-----|
| `Album::artist` | کلید Map: هنرمند |
| `album -> album` | مقدار اولیه: خود آلبوم |
| `maxBy(comparing(Album::sales))` | انتخاب پرفروش‌ترین آلبوم |

<a id="last-write-wins"></a>
### سیاست «آخرین مقدار برنده است»

<div dir="ltr">

```java
Collectors.toMap(
    keyMapper,
    valueMapper,
    (oldValue, newValue) -> newValue
)
```
</div>

> **نکته:** در Streamهای موازی یا منابعی که ترتیب مشخصی ندارند، «آخرین» مقدار ممکن است معنای موردانتظار شما را نداشته باشد.

> **نسخه چهارپارامتری `toMap`** نیز امکان تعیین نوع Map مقصد را می‌دهد (مثلاً `TreeMap`). خانواده `toConcurrentMap` برای تولید `ConcurrentMap` و پشتیبانی از پردازش موازی ارائه شده است.

[بازگشت به بالا](#top)

---

<a id="groupingby"></a>
## ۹. `groupingBy`: گروه‌بندی عناصر بر اساس یک ویژگی

<div dir="ltr">

```java
Map<Integer, List<String>> wordsByLength =
    words.stream()
        .collect(Collectors.groupingBy(String::length));
```
</div>

```
3 → [cat, dog]
4 → [java, code]
```

<a id="downstream"></a>
### تغییر نوع مقادیر گروه‌ها با Downstream Collector

**الف) نگهداری در Set:**

<div dir="ltr">

```java
Map<Integer, Set<String>> wordsByLength =
    words.stream()
        .collect(Collectors.groupingBy(
            String::length,
            Collectors.toSet()
        ));
```
</div>

**ب) شمارش عناصر هر گروه:**

<div dir="ltr">

```java
Map<Integer, Long> countByLength =
    words.stream()
        .collect(Collectors.groupingBy(
            String::length,
            Collectors.counting()
        ));
```
</div>

**ج) تعیین نوع Collection هر گروه:**

<div dir="ltr">

```java
Map<Integer, Set<String>> wordsByLength =
    words.stream()
        .collect(Collectors.groupingBy(
            String::length,
            Collectors.toCollection(TreeSet::new)
        ));
```
</div>

<a id="map-type"></a>
### تعیین نوع Map اصلی

<div dir="ltr">

```java
Map<Integer, Set<String>> wordsByLength =
    words.stream()
        .collect(Collectors.groupingBy(
            String::length,
            TreeMap::new,
            Collectors.toCollection(TreeSet::new)
        ));
```
</div>

| بخش | نقش |
|------|-----|
| `TreeMap::new` | نوع Map اصلی |
| `Collectors.toCollection(TreeSet::new)` | نوع Collection هر گروه |

<a id="partitioningby"></a>
### تفاوت `groupingBy` و `partitioningBy`

<div dir="ltr">

```java
Map<Boolean, List<String>> partitioned =
    words.stream()
        .collect(Collectors.partitioningBy(
            word -> word.length() > 5
        ));
```
</div>

| متد | نتیجه |
|------|-------|
| `groupingBy` | چندین گروه بر اساس ویژگی |
| `partitioningBy` | دقیقاً دو گروه: `true` و `false` |

> **برای اجرای موازی:** خانواده `groupingByConcurrent` نیز وجود دارد.

[بازگشت به بالا](#top)

---

<a id="all-collectors"></a>
## ۱۰. آیا باید تمام متدهای `Collectors` را یاد بگیریم؟

**خیر.** این یکی از نکات مهم کتاب است.

<div dir="ltr">

```java
long count = words.stream().count();
// ساده‌تر از:
long count = words.stream().collect(Collectors.counting());
```
</div>

<div dir="ltr">

```java
int sum = numbers.stream()
    .mapToInt(Integer::intValue)
    .sum();
```
</div>

> **نکته:** `counting()` زمانی مفید است که بخواهیم آن را به‌عنوان بخشی از یک عملیات گروه‌بندی انجام دهیم.

<div dir="ltr">

```java
Map<Integer, Long> countByLength =
    words.stream()
        .collect(Collectors.groupingBy(
            String::length,
            Collectors.counting()
        ));
```
</div>

> **نکته:** Collectorهای `mapping`، `filtering`، `flatMapping`، `reducing` و `collectingAndThen` امکان تبدیل‌ها و محاسبات بیشتر درون گروه‌بندی را فراهم می‌کنند.

[بازگشت به بالا](#top)

---

<a id="other-collectors"></a>
## ۱۱. سه Collector دیگر که ارزش شناختن دارند

<a id="minby-maxby"></a>
### الف) `minBy` و `maxBy`

<div dir="ltr">

```java
Optional<Album> bestSelling = albums.stream()
    .collect(Collectors.maxBy(
        Comparator.comparing(Album::sales)
    ));
```
</div>

**نسخه مستقیم‌تر:**

<div dir="ltr">

```java
Optional<Album> bestSelling = albums.stream()
    .max(Comparator.comparing(Album::sales));
```
</div>

> معمولاً نسخه مستقیم‌تر را ترجیح می‌دهیم، مگر اینکه به Collector نیاز داشته باشیم (مثلاً درون هر گروه).

<a id="joining"></a>
### ب) `joining`

<div dir="ltr">

```java
String result = words.stream().collect(Collectors.joining());
String result2 = words.stream().collect(Collectors.joining(", "));
String result3 = words.stream().collect(Collectors.joining(", ", "[", "]"));
```
</div>

```
Java, Streams, Collectors
[Java, Streams, Collectors]
```

[بازگشت به بالا](#top)

---

<a id="common-mistake"></a>
## ۱۲. یک اشتباه رایج: تصور اینکه هر Lambda بدون Side Effect است

<div dir="ltr">

```java
// ❌ از الگوی مطلوب Stream فاصله دارد
List<String> result = new ArrayList<>();

words.stream()
    .filter(word -> word.length() > 3)
    .forEach(result::add);
```
</div>

**راه طبیعی‌تر:**

<div dir="ltr">

```java
// ✅
List<String> result = words.stream()
    .filter(word -> word.length() > 3)
    .collect(Collectors.toList());
```
</div>

> **تفاوت مهم:** در نسخه اول، افزودن عناصر به `ArrayList` مشترک از طریق `forEach` موازی می‌تواند باعث مشکلات هم‌زمانی شود. در نسخه دوم، Collector مدیریت فرایند جمع‌آوری را بر عهده دارد.

> **نکته:** حتی استفاده از Collector نیز به این معنی نیست که هر Lambda داخل Pipeline به‌طور خودکار Pure است.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۱۳. جمع‌بندی نهایی Item 46

### اصول کلیدی

| # | اصل |
|---|-----|
| **۱** | **توابع بدون عارضه جانبی را ترجیح دهید** — نتیجه را بر اساس ورودی محاسبه کنند و وضعیت بیرونی را تغییر ندهند |
| **۲** | **از `forEach` برای انجام محاسبات اصلی استفاده نکنید** — ابتدا `collect` و Collectorهای مناسب را بررسی کنید |
| **۳** | **Collectorهای مهم را یاد بگیرید** — `toList`، `toSet`، `toMap`، `groupingBy` و `joining` |
| **۴** | **برای برخورد کلیدهای تکراری، سیاست مشخصی انتخاب کنید** — Merge Function در `toMap` |
| **۵** | **متد مستقیم‌تر را ترجیح دهید، مگر اینکه به Collector نیاز داشته باشید** — `count()` بهتر از `collect(counting())` |
| **۶** | **Downstream Collectorها ابزار قدرتمندی برای گروه‌بندی هستند** — `counting()`، `toSet()`، `toCollection()` درون `groupingBy` |

### جدول Collectorهای مهم

| Collector | نتیجه | کاربرد |
|-----------|-------|--------|
| `toList()` | `List` | جمع‌آوری ساده |
| `toSet()` | `Set` | حذف تکراری |
| `toCollection(factory)` | Collection دلخواه | کنترل نوع Collection |
| `toMap(k, v)` | `Map` | نگاشت ساده |
| `toMap(k, v, merge)` | `Map` با ادغام | مدیریت کلیدهای تکراری |
| `groupingBy(classifier)` | `Map<K, List<V>>` | گروه‌بندی |
| `groupingBy(classifier, downstream)` | `Map<K, R>` | گروه‌بندی با محاسبه |
| `groupingBy(classifier, factory, downstream)` | `Map<K, R>` با نوع دلخواه | کنترل کامل |
| `partitioningBy(predicate)` | `Map<Boolean, List<V>>` | تقسیم دودویی |
| `joining()` | `String` | اتصال رشته‌ها |
| `joining(sep)` | `String` | اتصال با جداکننده |
| `joining(sep, pre, post)` | `String` | اتصال با پیشوند و پسوند |
| `counting()` | `Long` | شمارش (مخصوصاً درون گروه) |
| `minBy(comp)` / `maxBy(comp)` | `Optional<T>` | انتخاب کمینه/بیشینه |

### ارتباط Item 46 با آیتم‌های قبلی

```
Item 42 → Lambdaها به‌جای Anonymous Classها
    ↓
Item 43 → Method Referenceها
    ↓
Item 44 → Functional Interface مناسب
    ↓
Item 45 → استفاده سنجیده از Stream
    ↓
Item 46 → محاسبات بدون Side Effect در Stream Pipeline
```

### نکته پایانی

> **اگر از Stream استفاده می‌کنید، اجازه دهید داده‌ها از یک مرحله به مرحله بعد منتقل شوند و نتیجه از طریق Pipeline ساخته شود؛ به‌جای اینکه محاسبات را داخل Lambdaها پنهان کنید و وضعیت متغیرهای بیرونی را تغییر دهید.**

---

[بازگشت به بالا](#top)

</div>