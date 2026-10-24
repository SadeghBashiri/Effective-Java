<div dir="rtl">

<a id="top"></a>

# آیتم ۴۷: Collection را به‌عنوان نوع بازگشتی به Stream ترجیح دهید

## (Prefer Collection to Stream as a return type)

> **پیام اصلی:** تا زمانی که امکان‌پذیر است، `Collection` را به `Stream` به‌عنوان نوع بازگشتی ترجیح بدهید.

این آیتم درباره‌ی یک تصمیم مهم در طراحی API است: وقتی متدی قرار است مجموعه‌ای از عناصر را برگرداند، بهتر است نوع بازگشتی آن `Collection` باشد، `Stream` باشد یا `Iterable`؟

نکته‌ی اصلی این است که نباید فقط به نحوه‌ی پیاده‌سازی متد فکر کنیم؛ باید به نحوه‌ی استفاده‌ی مصرف‌کنندگان API هم توجه کنیم.

---

## فهرست مطالب

- [۱. انتخاب نوع بازگشتی قبل و بعد از Java 8](#before-after-java8)
- [۲. چرا `Stream` مستقیماً در `for-each` قابل استفاده نیست؟](#stream-not-iterable)
  - [راه‌حل اول: تبدیل صریح به `Iterable`](#cast-solution)
  - [راه‌حل دوم: نوشتن یک adapter](#adapter-solution)
- [۳. جهت عکس: تبدیل `Iterable` به `Stream`](#iterable-to-stream)
- [۴. چرا `Collection` معمولاً بهترین انتخاب است؟](#collection-best)
- [۵. چه زمانی پیاده‌سازی یک Collection سفارشی منطقی است؟](#custom-collection)
  - [Power Set چیست؟](#power-set)
  - [ایده‌ی استفاده از بیت‌ها](#bit-idea)
  - [پیاده‌سازی نمونه‌ی کتاب](#powerset-impl)
  - [این مثال چه چیزی را ثابت می‌کند؟](#what-proves)
- [۶. محدودیت مهم Collection: متد `size()`](#size-limitation)
- [۷. چه زمانی باید Stream یا Iterable برگردانیم؟](#when-stream)
- [۸. مثال دوم کتاب: تولید تمام زیرلیست‌ها](#sublists)
  - [راه‌حل با Stream](#sublists-stream)
  - [نسخه‌ی نزدیک‌تر به حلقه‌های تو در تو](#nested-loops)
- [۹. راهنمای عملی انتخاب نوع بازگشتی](#decision-guide)
- [۱۰. جمع‌بندی Item 47](#final-summary)

[بازگشت به بالا](#top)

---

<a id="before-after-java8"></a>
## ۱. انتخاب نوع بازگشتی قبل و بعد از Java 8

قبل از Java 8، انتخاب معمولاً ساده‌تر بود:

| نوع بازگشتی | کاربرد معمول |
|-------------|--------------|
| `Collection` | وقتی به مجموعه‌ای از عناصر و عملیات مجموعه‌ای نیاز داریم |
| `List` | وقتی ترتیب عناصر و دسترسی بر اساس اندیس اهمیت دارد |
| `Set` | وقتی یکتایی عناصر اهمیت دارد |
| `Iterable` | وقتی عمدتاً پیمایش با `for-each` لازم است |
| آرایه | برای داده‌های primitive یا محدودیت‌های عملکردی |

با اضافه‌شدن Stream، انتخاب پیچیده‌تر شد:

<div dir="ltr">

```java
Stream<ProcessHandle> allProcesses();
```
</div>

| مصرف‌کننده | کد | مشکل |
|------------|-----|-------|
| طرفدار Stream | `allProcesses().filter(...).forEach(...)` | ✅ کار می‌کند |
| طرفدار `for-each` | `for (var p : allProcesses()) { }` | ❌ Compile Error |

> **مسئله‌ی اصلی Item 47:** برگرداندن Stream، مصرف‌کننده را به یک شیوه‌ی خاص از پیمایش محدود می‌کند.

[بازگشت به بالا](#top)

---

<a id="stream-not-iterable"></a>
## ۲. چرا `Stream` مستقیماً در `for-each` قابل استفاده نیست؟

حلقه‌ی enhanced `for` برای کار با آرایه‌ها و اشیایی طراحی شده که `Iterable` را پیاده‌سازی می‌کنند:

<div dir="ltr">

```java
public interface Iterable<T> {
    Iterator<T> iterator();
}
```
</div>

اگرچه `Stream` نیز متد `iterator()` را دارد، اما خودش `Iterable` نیست:

<div dir="ltr">

```java
// ❌ Compile Error
for (ProcessHandle process : ProcessHandle.allProcesses()::iterator) {
    System.out.println(process);
}
```
</div>

<a id="cast-solution"></a>
### راه‌حل اول: تبدیل صریح به `Iterable`

<div dir="ltr">

```java
for (ProcessHandle process :
        (Iterable<ProcessHandle>)
            ProcessHandle.allProcesses()::iterator) {
    System.out.println(process);
}
```
</div>

> **مشکل:** خوانایی خوبی ندارد و در کدهای واقعی تکرار آن باعث می‌شود API دشوارتر از آنچه باید باشد به نظر برسد.

<a id="adapter-solution"></a>
### راه‌حل دوم: نوشتن یک adapter

<div dir="ltr">

```java
public static <E> Iterable<E> iterableOf(Stream<E> stream) {
    return stream::iterator;
}

for (ProcessHandle process : iterableOf(ProcessHandle.allProcesses())) {
    System.out.println(process);
}
```
</div>

> **نکته:** اگر برای استفاده‌ی عادی از یک API دائماً به adapter نیاز داشته باشیم، شاید نوع بازگشتی آن بهترین انتخاب نباشد.

[بازگشت به بالا](#top)

---

<a id="iterable-to-stream"></a>
## ۳. جهت عکس: تبدیل `Iterable` به `Stream`

<div dir="ltr">

```java
public Iterable<String> getWords() {
    return List.of("Java", "Stream", "Collection");
}

// ❌ Compile Error
getWords().stream()
    .filter(word -> word.length() > 4)
    .forEach(System.out::println);
```
</div>

**راه‌حل:**

<div dir="ltr">

```java
public static <E> Stream<E> streamOf(Iterable<E> iterable) {
    return StreamSupport.stream(
        iterable.spliterator(),
        false
    );
}
```
</div>

| پارامتر | نقش |
|---------|-----|
| `spliterator()` | یک `Spliterator` برای پیمایش عناصر |
| `StreamSupport.stream(...)` | ساخت Stream از Spliterator |
| `false` | ساخت Stream ترتیبی (نه موازی) |

<div dir="ltr">

```java
streamOf(getWords())
    .filter(word -> word.length() > 4)
    .forEach(System.out::println);
```
</div>

> **همان سؤال طراحی:** چرا مصرف‌کننده باید برای کار با داده‌ها، آن‌ها را به رابط دیگری تبدیل کند؟

[بازگشت به بالا](#top)

---

<a id="collection-best"></a>
## ۴. چرا `Collection` معمولاً بهترین انتخاب است؟

رابط `Collection` هم `Iterable` را توسعه می‌دهد و هم متد `stream()` را در اختیار دارد:

```
              Collection<T>
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
    for-each              stream()
  پیمایش سنتی           پردازش تابعی
```

<div dir="ltr">

```java
public Collection<String> getNames() {
    return List.of("Ali", "Sara", "Reza");
}

// مصرف‌کننده ۱: پیمایش سنتی
for (String name : getNames()) {
    System.out.println(name);
}

// مصرف‌کننده ۲: پردازش تابعی
getNames().stream()
    .filter(name -> name.length() > 3)
    .map(String::toUpperCase)
    .forEach(System.out::println);
```
</div>

> **قاعده‌ی اصلی:** اگر می‌توانی مجموعه‌ای مناسب برگردانی، معمولاً `Collection` را انتخاب کن؛ اما برای پشتیبانی از دو شیوه‌ی پیمایش، بی‌دلیل تمام داده‌ها را در حافظه جمع نکن.

[بازگشت به بالا](#top)

---

<a id="custom-collection"></a>
## ۵. چه زمانی پیاده‌سازی یک Collection سفارشی منطقی است؟

<a id="power-set"></a>
### Power Set چیست؟

Power Set یک مجموعه، مجموعه‌ی تمام زیرمجموعه‌های آن است:

$$ S = \{a, b, c\} \implies \mathcal{P}(S) = \{\emptyset, \{a\}, \{b\}, \{c\}, \{a,b\}, \{a,c\}, \{b,c\}, \{a,b,c\}\} $$

$$ |\mathcal{P}(S)| = 2^n $$

> برای مجموعه‌ای با ۳۰ عنصر، Power Set بیش از یک میلیارد عضو دارد. ساختن تمام زیرمجموعه‌ها و نگهداری آن‌ها در حافظه، هزینه‌ی بسیار زیادی خواهد داشت.

<a id="bit-idea"></a>
### ایده‌ی استفاده از بیت‌ها

برای مجموعه‌ی `{a, b, c}`:

| عدد | دودویی | زیرمجموعه |
|-----|--------|-----------|
| 0 | `000` | `{}` |
| 1 | `001` | `{a}` |
| 2 | `010` | `{b}` |
| 3 | `011` | `{a, b}` |
| 4 | `100` | `{c}` |
| 5 | `101` | `{a, c}` |
| 6 | `110` | `{b, c}` |
| 7 | `111` | `{a, b, c}` |

<a id="powerset-impl"></a>
### پیاده‌سازی نمونه‌ی کتاب

<div dir="ltr">

```java
public class PowerSet {
    public static <E> Collection<Set<E>> of(Set<E> s) {
        List<E> src = new ArrayList<>(s);

        if (src.size() > 30) {
            throw new IllegalArgumentException("Set too big " + s);
        }

        return new AbstractList<Set<E>>() {
            @Override
            public int size() {
                return 1 << src.size();
            }

            @Override
            public boolean contains(Object o) {
                return o instanceof Set
                    && src.containsAll((Set<?>) o);
            }

            @Override
            public Set<E> get(int index) {
                Set<E> result = new HashSet<>();

                for (int i = 0; index != 0; i++, index >>= 1) {
                    if ((index & 1) == 1) {
                        result.add(src.get(i));
                    }
                }

                return result;
            }
        };
    }
}
```
</div>

> **نکته:** زیرمجموعه‌ها در `get()` و در زمان درخواست تولید می‌شوند. در نتیجه، لازم نیست تمام $2^n$ زیرمجموعه در حافظه ذخیره شوند.

> **نکته Production:** این کد نمونه‌ی آموزشی است. برای استفاده‌ی واقعی باید ورودی اندیس `get()` (شامل منفی و خارج از محدوده) به‌درستی اعتبارسنجی شود.

<a id="what-proves"></a>
### این مثال چه چیزی را ثابت می‌کند؟

| رویکرد | پیمایش `for-each` | `stream()` | مصرف حافظه |
|--------|---------------------|------------|-------------|
| `Stream<Set<E>>` | ❌ | ✅ | کم |
| Collection استاندارد با همه زیرمجموعه‌ها | ✅ | ✅ | بسیار زیاد |
| Collection سفارشی (تولید در زمان نیاز) | ✅ | ✅ | کم |

[بازگشت به بالا](#top)

---

<a id="size-limitation"></a>
## ۶. محدودیت مهم Collection: متد `size()`

متد `size()` در `Collection` مقدار `int` برمی‌گرداند.

$$ 2^{31} > 2^{31}-1 $$

| محدودیت | توضیح |
|---------|-------|
| حداکثر مقدار `int` | `2^31 - 1` = `Integer.MAX_VALUE` |
| رفتار `Collection` | ممکن است برای مجموعه‌های بسیار بزرگ `Integer.MAX_VALUE` برگرداند |
| پیامد | این مقدار دیگر اندازه‌ی دقیق مجموعه نیست |

> **نتیجه:** برای توالی‌های نامتناهی یا توالی‌هایی که تعداد عناصرشان پیشاپیش مشخص نیست، `Collection` ممکن است انتخاب مناسبی نباشد.

[بازگشت به بالا](#top)

---

<a id="when-stream"></a>
## ۷. چه زمانی باید Stream یا Iterable برگردانیم؟

| شرایط | انتخاب پیشنهادی |
|-------|------------------|
| عناصر فقط برای پردازش در یک pipeline استفاده می‌شوند | `Stream` |
| هدف اصلی پیمایش با `for-each` است | `Iterable` |
| API عمومی است و هر دو شیوه‌ی استفاده اهمیت دارند | ابتدا `Collection` را بررسی کن |
| Collection عملی نیست | `Stream` یا `Iterable` بر اساس ماهیت داده‌ها |

> **نکته:** یک API می‌تواند دو متد جداگانه هم ارائه کند؛ یکی برای پیمایش و دیگری برای پردازش Stream. اما این کار باید به‌اندازه‌ی کافی ارزشمند باشد تا پیچیدگی بیشتر API را توجیه کند.

[بازگشت به بالا](#top)

---

<a id="sublists"></a>
## ۸. مثال دوم کتاب: تولید تمام زیرلیست‌ها

برای فهرست `[a, b, c]`:

```
[a], [a, b], [a, b, c], [b], [b, c], [c], []
```

تعداد زیرلیست‌های متوالی شامل فهرست خالی:

$$ 1 + \frac{n(n+1)}{2} $$

<a id="sublists-stream"></a>
### راه‌حل با Stream

<div dir="ltr">

```java
public class SubLists {
    public static <E> Stream<List<E>> of(List<E> list) {
        return Stream.concat(
            Stream.of(Collections.emptyList()),
            prefixes(list).flatMap(SubLists::suffixes)
        );
    }

    private static <E> Stream<List<E>> prefixes(List<E> list) {
        return IntStream.rangeClosed(1, list.size())
            .mapToObj(end -> list.subList(0, end));
    }

    private static <E> Stream<List<E>> suffixes(List<E> list) {
        return IntStream.range(0, list.size())
            .mapToObj(start -> list.subList(start, list.size()));
    }
}
```
</div>

**سه مفهوم کلیدی:**

| مفهوم | توضیح | مثال برای `[a, b, c]` |
|--------|-------|----------------------|
| **Prefix** | زیرلیستی که از ابتدا شروع می‌شود | `[a]`، `[a,b]`، `[a,b,c]` |
| **Suffix** | زیرلیستی که به انتها ختم می‌شود | `[a,b,c]`، `[b,c]`، `[c]` |
| **ارتباط** | هر زیرلیست غیرخالی = پسوند یک پیشوند | `[b]` = پسوند `[a, b]` |

> **نکته:** `subList()` معمولاً یک نمایی از فهرست اصلی برمی‌گرداند، نه یک کپی مستقل. تغییرات بعدی در فهرست اصلی می‌تواند روی این زیرلیست‌ها اثر بگذارد.

<a id="nested-loops"></a>
### نسخه‌ی نزدیک‌تر به حلقه‌های تو در تو

<div dir="ltr">

```java
public static <E> Stream<List<E>> of(List<E> list) {
    return IntStream.range(0, list.size())
        .mapToObj(start ->
            IntStream.rangeClosed(start + 1, list.size())
                .mapToObj(end -> list.subList(start, end))
        )
        .flatMap(x -> x);
}
```
</div>

> **نکته:** این نسخه زیرلیست‌های غیرخالی را تولید می‌کند. برای اضافه‌کردن فهرست خالی، می‌توان از `Stream.concat` استفاده کرد.

[بازگشت به بالا](#top)

---

<a id="decision-guide"></a>
## ۹. راهنمای عملی انتخاب نوع بازگشتی

| شرایط | انتخاب پیشنهادی |
|--------|------------------|
| عناصر کم هستند و ساخت مجموعه هزینه‌ی قابل‌قبولی دارد | `List`، `Set` یا `Collection` |
| مصرف‌کنندگان به پیمایش و Stream هر دو نیاز دارند | معمولاً `Collection` |
| هدف مشخصاً پردازش یک pipeline است | `Stream` |
| هدف عمدتاً پیمایش با `for-each` است | `Iterable` |
| تعداد عناصر زیاد است اما می‌توان در صورت نیاز محاسبه کرد | Collection سفارشی، `Stream` یا `Iterable` |
| توالی نامتناهی است یا اندازه‌ی آن به‌سادگی قابل‌محاسبه نیست | معمولاً `Stream` یا `Iterable` |
| ساخت Collection باعث مصرف بیش‌ازحد حافظه می‌شود | از materialize کردن تمام عناصر خودداری کن |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۱۰. جمع‌بندی Item 47

> **پیام این آیتم صرفاً این نیست که همیشه `Collection` برگردانیم؛ بلکه تأکید می‌کند که نوع بازگشتی یک API باید نیازهای مصرف‌کنندگان را پوشش دهد، نه اینکه فقط پیاده‌سازی داخلی را ساده کند.**

### اصول کلیدی

| # | اصل |
|---|-----|
| **۱** | `Stream` جایگزین کامل `Iterable` نیست؛ این دو روش استفاده‌ی متفاوتی را پشتیبانی می‌کنند |
| **۲** | `Collection` معمولاً برای API عمومی مناسب‌تر است، چون هم `for-each` و هم `stream()` را پشتیبانی می‌کند |
| **۳** | اگر داده‌ها کم هستند، از پیاده‌سازی‌های استانداردی مثل `ArrayList` و `HashSet` استفاده کن |
| **۴** | اگر داده‌ها زیادند، صرفاً برای برگرداندن Collection همه‌ی عناصر را در حافظه ذخیره نکن؛ Collection سفارشی گزینه‌ی مناسبی است |
| **۵** | اگر ساخت Collection عملی نیست، `Stream` یا `Iterable` را بر اساس شیوه‌ی استفاده‌ی طبیعی از داده‌ها انتخاب کن |
| **۶** | در APIهای عمومی، به تجربه‌ی مصرف‌کننده، خوانایی، هزینه‌ی حافظه و عملکرد توجه کن |

### جدول مقایسه نهایی

| نوع بازگشتی | `for-each` | `stream()` | مصرف حافظه | مناسب برای |
|-------------|------------|------------|-------------|-------------|
| `Collection` | ✅ | ✅ | متوسط | API عمومی، داده‌های با اندازه متوسط |
| `List` / `Set` | ✅ | ✅ | متوسط | ترتیب یا یکتایی مهم است |
| `Iterable` | ✅ | ❌ (نیاز به adapter) | کم | پیمایش ساده |
| `Stream` | ❌ (نیاز به adapter) | ✅ | کم | پردازش pipeline |
| Collection سفارشی | ✅ | ✅ | کم | داده‌های بسیار بزرگ، تولید در زمان نیاز |

### ارتباط با Item 45

```
Item 45 → Stream و حلقه‌ها را هوشمندانه ترکیب کنیم
    ↓
Item 47 → هنگام طراحی API، نوعی را برگردان که این انتخاب را برای مصرف‌کننده آسان نگه دارد
```

### نکته پایانی

> **طراحی خوب API یعنی نه‌تنها داده‌ی درست را ارائه کنیم، بلکه راه استفاده‌ی درست و طبیعی از آن داده را نیز در اختیار مصرف‌کننده بگذاریم.**

---

[بازگشت به بالا](#top)

</div>