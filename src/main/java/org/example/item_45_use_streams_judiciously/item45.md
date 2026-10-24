<div dir="rtl">

<a id="top"></a>

# آیتم ۴۵: استفاده سنجیده از Streamها

## (Use streams judiciously)

> **پیام اصلی این آیتم:** استفاده از Stream API لزوماً کد را بهتر نمی‌کند؛ کد خوب، کدی است که خواندن، درک کردن و نگهداری آن آسان باشد.

از زمان معرفی Stream API در Java 8، بسیاری از برنامه‌نویسان به این نتیجه رسیده‌اند که بهتر است تمام حلقه‌های `for` و `while` خود را به Stream تبدیل کنند. اما Joshua Bloch در این آیتم هشدار می‌دهد که این کار همیشه درست نیست. بعضی عملیات با Stream بسیار خواناتر می‌شوند، بعضی با حلقه‌های معمولی، و بسیاری از مسائل نیز با ترکیب هوشمندانه این دو روش بهترین نتیجه را می‌دهند.

---

## فهرست مطالب

- [۱. Stream API چیست و چگونه کار می‌کند؟](#what-is-stream)
  - [سه بخش اصلی یک Stream Pipeline](#pipeline)
  - [نکته مهم: Streamها به‌صورت Lazy ارزیابی می‌شوند](#lazy)
- [۲. مثال اصلی کتاب: پیدا کردن گروه‌های Anagram](#anagram-example)
  - [نسخه اول: پیاده‌سازی با حلقه‌های معمولی](#iterative)
- [۳. نسخه دوم: وقتی Streamها را بیش از حد استفاده می‌کنیم](#overuse)
- [۴. نسخه سوم: تعادل میان Stream و Iteration](#balanced)
  - [مرحله اول: خواندن کلمات فایل](#step1)
  - [مرحله دوم: گروه‌بندی کلمات](#step2)
  - [مرحله سوم: گرفتن Stream از گروه‌ها](#step3)
  - [مرحله چهارم: حذف گروه‌های کوچک](#step4)
  - [مرحله پنجم: چاپ نتیجه](#step5)
  - [چرا این نسخه بهتر است؟](#why-better)
- [۵. چرا متدهای کمکی در کدهای Stream اهمیت بیشتری دارند؟](#helper-methods)
- [۶. دام استفاده از Stream برای پردازش `char`](#char-trap)
- [۷. چه کارهایی با Iteration بهتر انجام می‌شوند؟](#when-iteration)
  - [تفاوت اول: تغییر متغیرهای محلی](#local-variables)
  - [تفاوت دوم: کنترل جریان برنامه](#flow-control)
- [۸. چه کارهایی با Stream بهتر انجام می‌شوند؟](#when-stream)
- [۹. یک مشکل ظریف در Streamها: دسترسی به مقادیر مراحل قبلی](#access-previous)
  - [مثال کتاب: اعداد اول مرسن (Mersenne Primes)](#mersenne)
  - [حالا اگر بخواهیم توان را نیز چاپ کنیم چه می‌شود؟](#print-power)
- [۱۰. مثال پایانی کتاب: ساخت یک دسته کارت با Cartesian Product](#cartesian)
  - [روش اول: حلقه‌های تو در تو](#nested-loops)
  - [روش دوم: استفاده از Stream و `flatMap`](#flatmap)
  - [کدام نسخه بهتر است؟](#which-better)
- [۱۱. چطور تصمیم بگیریم از Stream استفاده کنیم یا از حلقه؟](#decision)
- [جمع‌بندی نهایی Item 45](#final-summary)

[بازگشت به بالا](#top)

---

<a id="what-is-stream"></a>
## ۱. Stream API چیست و چگونه کار می‌کند؟

Stream در جاوا، دنباله‌ای از عناصر است که می‌توانیم روی آن‌ها عملیات مختلفی انجام دهیم؛ مانند تبدیل عناصر، فیلتر کردن، گروه‌بندی، جست‌وجو و محاسبه نتیجه نهایی.

**منابع Stream:**
- Collectionها مانند `List` و `Set`
- آرایه‌ها
- فایل‌ها
- تولیدکننده‌های اعداد تصادفی
- Streamهای دیگر

<div dir="ltr">

```java
List<String> names = List.of("Ali", "Sara", "Amir", "Saba");

names.stream()
     .filter(name -> name.startsWith("S"))
     .map(String::toUpperCase)
     .forEach(System.out::println);

// خروجی:
// SARA
// SABA
```
</div>

<a id="pipeline"></a>
### سه بخش اصلی یک Stream Pipeline

| بخش | توضیح | مثال |
|------|-------|------|
| **Source** | منبع داده | `names.stream()` |
| **Intermediate Operations** | عملیات میانی (صفر یا چند مرحله) | `filter(...)` → `map(...)` |
| **Terminal Operation** | عملیات نهایی | `forEach(...)` |

**عملیات میانی متداول:**

| عملیات | کاربرد |
|--------|--------|
| `filter` | حذف عناصر نامطلوب |
| `map` | تبدیل هر عنصر به مقداری دیگر |
| `sorted` | مرتب‌سازی عناصر |
| `distinct` | حذف عناصر تکراری |
| `flatMap` | تبدیل هر عنصر به یک Stream و سپس تخت‌کردن نتایج |

**عملیات نهایی متداول:**

| عملیات | کاربرد |
|--------|--------|
| `collect` | جمع‌آوری نتایج |
| `forEach` | انجام عملیات روی هر عنصر |
| `count` | شمارش عناصر |
| `findFirst` | یافتن اولین عنصر |
| `reduce` | ترکیب عناصر برای تولید یک نتیجه |

<a id="lazy"></a>
### نکته مهم: Streamها به‌صورت Lazy ارزیابی می‌شوند

<div dir="ltr">

```java
Stream<String> stream = names.stream()
    .filter(name -> name.startsWith("S"));
// در این نقطه، فیلتر کردن عناصر هنوز اجرا نشده است

stream.forEach(System.out::println);  // اکنون اجرا می‌شود
```
</div>

<div dir="ltr">

```java
names.stream()
     .filter(name -> name.startsWith("S"));
// ❌ هیچ نتیجه قابل‌مشاهده‌ای تولید نمی‌کند؛ چون عملیات نهایی ندارد
```
</div>

> **نکته:** Streamها به‌طور پیش‌فرض ترتیبی (Sequential) اجرا می‌شوند. `parallel()` به این معنی نیست که عملکرد برنامه حتماً بهتر خواهد شد (Item 48).

[بازگشت به بالا](#top)

---

<a id="anagram-example"></a>
## ۲. مثال اصلی کتاب: پیدا کردن گروه‌های Anagram

دو کلمه زمانی Anagram هستند که از حروف یکسانی تشکیل شده باشند:

```
staple → aelpst
petals → aelpst
pastel → aelpst
```

ساختار داده:

```
"aelpst" → ["pastel", "petals", "staple"]
```

<a id="iterative"></a>
### نسخه اول: پیاده‌سازی با حلقه‌های معمولی

<div dir="ltr">

```java
public class Anagrams {
    public static void main(String[] args) throws IOException {
        File dictionary = new File(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);

        Map<String, Set<String>> groups = new HashMap<>();

        try (Scanner scanner = new Scanner(dictionary)) {
            while (scanner.hasNext()) {
                String word = scanner.next();
                groups.computeIfAbsent(
                    alphabetize(word),
                    unused -> new TreeSet<>()
                ).add(word);
            }
        }

        for (Set<String> group : groups.values()) {
            if (group.size() >= minGroupSize) {
                System.out.println(group.size() + ": " + group);
            }
        }
    }

    private static String alphabetize(String s) {
        char[] chars = s.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
```
</div>

**نکات مهم:**

| بخش | توضیح |
|------|-------|
| `Map<String, Set<String>>` | کلید: شکل مرتب‌شده حروف، مقدار: مجموعه کلمات هم‌گروه |
| `computeIfAbsent` | نیاز به شرط‌های جداگانه برای بررسی وجود کلید را از بین می‌برد |
| `TreeSet` | عناصر را مرتب نگه می‌دارد و از تکراری جلوگیری می‌کند |
| `alphabetize` | متد مستقل با هدف مشخص |

[بازگشت به بالا](#top)

---

<a id="overuse"></a>
## ۳. نسخه دوم: وقتی Streamها را بیش از حد استفاده می‌کنیم

<div dir="ltr">

```java
try (Stream<String> words = Files.lines(dictionary)) {
    words.collect(
        groupingBy(word -> word.chars()
            .sorted()
            .collect(
                StringBuilder::new,
                (sb, c) -> sb.append((char) c),
                StringBuilder::append
            ).toString()
        )
    ).values().stream()
     .filter(group -> group.size() >= minGroupSize)
     .map(group -> group.size() + ": " + group)
     .forEach(System.out::println);
}
```
</div>

به این قسمت توجه کنید:

<div dir="ltr">

```java
word.chars()
    .sorted()
    .collect(
        StringBuilder::new,
        (sb, c) -> sb.append((char) c),
        StringBuilder::append
    ).toString()
```
</div>

**مشکل:** جزئیات یک عملیات ساده را درون یک Pipeline پیچیده پنهان کرده‌ایم. کد کوتاه‌تر شده، اما الزاماً خواناتر نشده است.

> **درس مهم:** تعداد خطوط کمتر، به‌تنهایی معیار کیفیت کد نیست.

[بازگشت به بالا](#top)

---

<a id="balanced"></a>
## ۴. نسخه سوم: تعادل میان Stream و Iteration

<div dir="ltr">

```java
public class Anagrams {
    public static void main(String[] args) throws IOException {
        Path dictionary = Paths.get(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);

        try (Stream<String> words = Files.lines(dictionary)) {
            words.collect(
                groupingBy(word -> alphabetize(word))
            ).values().stream()
             .filter(group -> group.size() >= minGroupSize)
             .forEach(group ->
                 System.out.println(group.size() + ": " + group)
             );
        }
    }

    private static String alphabetize(String s) {
        char[] chars = s.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
```
</div>

<a id="step1"></a>
### مرحله اول: خواندن کلمات فایل

<div dir="ltr">

```java
Stream<String> words = Files.lines(dictionary);
```
</div>

`try-with-resources` باعث می‌شود Stream و منبع فایل پس از پایان کار بسته شوند.

<a id="step2"></a>
### مرحله دوم: گروه‌بندی کلمات

<div dir="ltr">

```java
words.collect(groupingBy(word -> alphabetize(word)))
```
</div>

کلمات بر اساس خروجی `alphabetize` گروه‌بندی می‌شوند.

<a id="step3"></a>
### مرحله سوم: گرفتن Stream از گروه‌ها

<div dir="ltr">

```java
.values().stream()
```
</div>

اکنون عناصر Stream، خود گروه‌های کلمات هستند.

<a id="step4"></a>
### مرحله چهارم: حذف گروه‌های کوچک

<div dir="ltr">

```java
.filter(group -> group.size() >= minGroupSize)
```
</div>

<a id="step5"></a>
### مرحله پنجم: چاپ نتیجه

<div dir="ltr">

```java
.forEach(group -> System.out.println(group.size() + ": " + group));
```
</div>

<a id="why-better"></a>
### چرا این نسخه بهتر است؟

| بخش | مسئولیت |
|------|---------|
| `Files.lines` | خواندن فایل |
| `groupingBy` | گروه‌بندی |
| `filter` | اعمال شرط |
| `forEach` | چاپ نتیجه |
| `alphabetize` | مرتب‌کردن حروف یک کلمه |

> **درس مهم:** استفاده مناسب از Stream به معنی تبدیل تمام عملیات به Lambda نیست؛ گاهی یک متد ساده و نام‌گذاری‌شده، بهترین انتخاب است.

[بازگشت به بالا](#top)

---

<a id="helper-methods"></a>
## ۵. چرا متدهای کمکی در کدهای Stream اهمیت بیشتری دارند؟

در حلقه‌های معمولی می‌توانیم از متغیرهای موقت و بلوک‌های کد استفاده کنیم:

<div dir="ltr">

```java
for (String word : words) {
    String sortedWord = alphabetize(word);
    // ادامه پردازش
}
```
</div>

اما در Pipelineهای Stream، عملیات‌ها به یکدیگر متصل می‌شوند:

<div dir="ltr">

```java
words.stream()
     .map(word -> alphabetize(word))
     .filter(word -> word.length() > 5);
```
</div>

**بهتر است عملیات معنادار را در متدهای مستقل قرار دهیم:**

<div dir="ltr">

```java
// به‌جای:
.map(word -> {
    char[] chars = word.toCharArray();
    Arrays.sort(chars);
    return new String(chars);
})

// از این استفاده کنیم:
.map(Anagrams::alphabetize)
```
</div>

> **مزیت اصلی:** نام متد به خواننده می‌گوید عملیات چه هدفی دارد.

> **نکته درباره نام پارامترها:** `group -> group.size() >= minGroupSize` بهتر از `x -> x.size() >= minGroupSize` است.

[بازگشت به بالا](#top)

---

<a id="char-trap"></a>
## ۶. دام استفاده از Stream برای پردازش `char`

<div dir="ltr">

```java
"Hello world!".chars()
    .forEach(System.out::print);
```
</div>

**خروجی واقعی:**

```
721011081081113211911111410810833
```

**چرا؟** `chars()` یک `IntStream` برمی‌گرداند، نه یک Stream از مقادیر `char`. مقدار عددی `H` برابر با `72` است.

**راه‌حل:**

<div dir="ltr">

```java
"Hello world!".chars()
    .forEach(c -> System.out.print((char) c));
// خروجی: Hello world!
```
</div>

| نکته | توضیح |
|------|-------|
| `IntStream` | `chars()` مقادیر را به‌صورت `int` برمی‌گرداند |
| `CharStream` مستقل | در جاوا وجود ندارد |
| نتیجه | پردازش کاراکترها با Stream گاهی به تبدیل‌های اضافی منجر می‌شود |

> **قاعده عملی:** صرف اینکه یک عملیات را می‌توان با Stream پیاده‌سازی کرد، به این معنی نیست که باید این کار را انجام دهیم.

[بازگشت به بالا](#top)

---

<a id="when-iteration"></a>
## ۷. چه کارهایی با Iteration بهتر انجام می‌شوند؟

<a id="local-variables"></a>
### تفاوت اول: تغییر متغیرهای محلی

<div dir="ltr">

```java
int total = 0;
for (int number : numbers) {
    total += number;  // ✅
}
```
</div>

<div dir="ltr">

```java
int total = 0;
numbers.forEach(number -> total += number);  // ❌ Compile Error
```
</div>

**راه‌حل طبیعی Stream:**

<div dir="ltr">

```java
int total = numbers.stream()
    .mapToInt(Integer::intValue)
    .sum();
```
</div>

<a id="flow-control"></a>
### تفاوت دوم: کنترل جریان برنامه

<div dir="ltr">

```java
for (String word : words) {
    if (word.isEmpty()) {
        continue;  // ✅
    }
    if (word.equals("STOP")) {
        break;  // ✅
    }
    process(word);
}
```
</div>

> **در Lambda:** نمی‌توانیم به‌طور مستقیم از `break` یا `continue` برای کنترل حلقه بیرونی استفاده کنیم و `return` نیز فقط از خود Lambda برمی‌گردد.

> **نتیجه:** زمانی که منطق برنامه به کنترل جریان صریح وابسته است، حلقه‌های معمولی اغلب انتخاب بهتری هستند.

[بازگشت به بالا](#top)

---

<a id="when-stream"></a>
## ۸. چه کارهایی با Stream بهتر انجام می‌شوند؟

| عملیات | مثال | کاربرد |
|--------|------|--------|
| **تبدیل یکنواخت** | `map(String::length)` | تبدیل همه عناصر با یک تابع |
| **فیلترکردن** | `filter(word -> word.length() > 5)` | نگه‌داشتن عناصر مطابق شرط |
| **ترکیب عناصر** | `sum`، `min`، `max`، `reduce` | جمع، حداقل، حداکثر |
| **جمع‌آوری و گروه‌بندی** | `collect(groupingBy(String::length))` | تبدیل به Collection یا گروه‌بندی |
| **جست‌وجو** | `findFirst`، `anyMatch`، `allMatch` | یافتن یا بررسی عنصر |

<div dir="ltr">

```java
boolean hasEmptyWord = words.stream()
    .anyMatch(String::isEmpty);
```
</div>

[بازگشت به بالا](#top)

---

<a id="access-previous"></a>
## ۹. یک مشکل ظریف در Streamها: دسترسی به مقادیر مراحل قبلی

<div dir="ltr">

```java
numbers.stream()
    .map(number -> number * 2)
    .forEach(result -> System.out.println(result));
```
</div>

پس از `map`، مقدار اصلی دیگر به‌عنوان عنصر جاری در دسترس نیست.

<a id="mersenne"></a>
### مثال کتاب: اعداد اول مرسن (Mersenne Primes

تعریف:

$$ M_p = 2^p - 1 $$

<div dir="ltr">

```java
static Stream<BigInteger> primes() {
    return Stream.iterate(TWO, BigInteger::nextProbablePrime);
}
```
</div>

<div dir="ltr">

```java
primes()
    .map(p -> TWO.pow(p.intValueExact()).subtract(ONE))
    .filter(mersenne -> mersenne.isProbablePrime(50))
    .limit(20)
    .forEach(System.out::println);
```
</div>

| مرحله | عملیات |
|--------|--------|
| ۱ | تولید اعداد اول $p$ |
| ۲ | محاسبه $2^p - 1$ |
| ۳ | بررسی اول بودن عدد مرسن حاصل |
| ۴ | محدودکردن نتیجه به ۲۰ عدد |
| ۵ | چاپ نتایج |

> **نکته:** `isProbablePrime(50)` یک آزمون احتمالی اول‌بودن است، نه اثبات قطعی.

<a id="print-power"></a>
### حالا اگر بخواهیم توان را نیز چاپ کنیم چه می‌شود؟

پس از تبدیل $p$ به $2^p - 1$، دیگر خود $p$ را نداریم.

**راه‌حل کتاب:** بازیابی $p$ از خود عدد مرسن:

<div dir="ltr">

```java
.forEach(mp -> System.out.println(mp.bitLength() + ": " + mp));
```
</div>

**چرا `bitLength()` کار می‌کند؟**

$$ M_p = 2^p - 1 $$

نمایش دودویی این عدد شامل $p$ بیت است که همگی `1` هستند:

| p | $M_p$ | دودویی | bitLength |
|---|-------|--------|-----------|
| ۳ | ۷ | `111` | ۳ |
| ۵ | ۳۱ | `11111` | ۵ |

> **نکته:** این راهکار فقط زمانی مناسب است که بازیابی مقدار اولیه از نتیجه، از نظر منطقی معتبر باشد.

> **نکته درباره نام‌گذاری:** نام `primes()` یک اسم جمع است که به خواننده می‌گوید نتیجه، دنباله‌ای از اعداد اول است.

[بازگشت به بالا](#top)

---

<a id="cartesian"></a>
## ۱۰. مثال پایانی کتاب: ساخت یک دسته کارت با Cartesian Product

- مجموعه‌ای از خال‌های کارت (`Suit`)
- مجموعه‌ای از رتبه‌های کارت (`Rank`)

اگر ۴ خال و ۱۳ رتبه داشته باشیم:

$$ 4 \times 13 = 52 $$

<a id="nested-loops"></a>
### روش اول: حلقه‌های تو در تو

<div dir="ltr">

```java
private static List<Card> newDeck() {
    List<Card> result = new ArrayList<>();

    for (Suit suit : Suit.values()) {
        for (Rank rank : Rank.values()) {
            result.add(new Card(suit, rank));
        }
    }

    return result;
}
```
</div>

> **مزیت اصلی:** سادگی و آشنایی برای اکثر برنامه‌نویسان جاوا.

<a id="flatmap"></a>
### روش دوم: استفاده از Stream و `flatMap`

<div dir="ltr">

```java
private static List<Card> newDeck() {
    return Stream.of(Suit.values())
        .flatMap(suit ->
            Stream.of(Rank.values())
                .map(rank -> new Card(suit, rank))
        )
        .collect(Collectors.toList());
}
```
</div>

| عملیات | نقش |
|--------|-----|
| `map` | هر رتبه را به یک کارت تبدیل می‌کند |
| `flatMap` | برای هر خال یک Stream از کارت‌ها می‌سازد و سپس Streamها را تخت می‌کند |

<a id="which-better"></a>
### کدام نسخه بهتر است؟

| معیار | حلقه‌های تو در تو | Stream و `flatMap` |
|--------|-------------------|---------------------|
| آشنایی برای بیشتر برنامه‌نویسان | معمولاً بیشتر | وابسته به تسلط بر Stream |
| خوانایی برای عملیات ساده | بسیار خوب | خوب، اگر با Stream آشنا باشید |
| فشردگی کد | معمولاً کمتر | معمولاً بیشتر |
| بیان عملیات به سبک تابعی | کمتر | بیشتر |

> **اصل نهایی:** کدی را انتخاب کنید که توسعه‌دهندگان مسئول نگهداری آن، راحت‌تر بتوانند بخوانند و تغییر دهند.

[بازگشت به بالا](#top)

---

<a id="decision"></a>
## ۱۱. چطور تصمیم بگیریم از Stream استفاده کنیم یا از حلقه؟

| وضعیت | پیشنهاد |
|--------|---------|
| می‌خواهم داده‌ها را تبدیل یا فیلتر کنم | Stream (`map`، `filter`، `distinct`) |
| می‌خواهم داده‌ها را گروه‌بندی یا جمع‌آوری کنم | Stream (`collect`، `groupingBy`) |
| به `break`، `continue` یا تغییر متغیرهای محلی نیاز دارم | حلقه |
| منطق چندمرحله‌ای پیچیده‌ای دارم که توضیحش با حلقه آسان‌تر است | حلقه |
| هر دو روش ممکن است و مطمئن نیستم کدام بهتر است | Stream را امتحان کنید |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی Item 45

> **مهم‌ترین درس این آیتم این نیست که Stream بهتر از حلقه است یا حلقه بهتر از Stream؛ بلکه این است که هر ابزار برای نوع خاصی از مسئله مناسب‌تر است.**

### اصول کلیدی

| # | اصل |
|---|-----|
| **۱** | از Stream برای تبدیل، فیلترکردن، جست‌وجو، گروه‌بندی و جمع‌آوری داده‌ها استفاده کنید |
| **۲** | برای منطق پیچیده‌ای که به کنترل جریان صریح وابسته است، حلقه‌های معمولی را در نظر بگیرید |
| **۳** | از تبدیل تمام حلقه‌ها به Stream صرفاً به دلیل مدرن‌تر بودن آن خودداری کنید |
| **۴** | برای خواناتر شدن Pipelineها، از متدهای کمکی با نام‌های معنادار استفاده کنید |
| **۵** | اگر هر دو روش مناسب‌اند، روشی را انتخاب کنید که برای تیم شما قابل‌فهم‌تر و نگهداری‌پذیرتر است |
| **۶** | از Streamهای موازی بدون بررسی مناسب‌بودن مسئله و شرایط اجرایی استفاده نکنید |

### ارتباط با آیتم‌های قبلی

```
Item 42 → Lambdaها به‌درستی استفاده کنیم
    ↓
Item 43 → Method Referenceها
    ↓
Item 44 → انتخاب Functional Interface مناسب
    ↓
Item 45 → حتی با تمام این ابزارها، باید در انتخاب شیوه پیاده‌سازی سنجیده عمل کنیم
```

### نکته پایانی

> **هدف، نوشتن کدی نیست که بیشترین استفاده را از قابلیت‌های مدرن جاوا ببرد؛ هدف، نوشتن کدی است که با کمترین پیچیدگی، واضح‌ترین بیان را از منطق برنامه ارائه کند.**

---

[بازگشت به بالا](#top)

</div>