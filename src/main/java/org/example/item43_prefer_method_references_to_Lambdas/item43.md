<div dir="rtl">

<a id="top"></a>

# آیتم ۴۳: ارجاع به متدها را به Lambdaها ترجیح دهید

## (Prefer Method References to Lambdas)

در Item 42 یاد گرفتیم که چرا معمولاً باید Lambda Expression را به Anonymous Class ترجیح دهیم. حالا Joshua Bloch یک قدم جلوتر می‌رود و می‌گوید:

> **اگر بتوانیم همان رفتار را با یک Method Reference کوتاه‌تر و واضح‌تر بیان کنیم، بهتر است به‌جای lambda از آن استفاده کنیم.**

هدف این آیتم فقط کاهش تعداد کاراکترها نیست؛ بلکه حذف جزئیاتی است که برای فهمیدن منطق برنامه ضروری نیستند.

---

## فهرست مطالب

- [۱. مثال اصلی: استفاده از `Integer::sum` به‌جای Lambda](#main-example)
    - [نسخهٔ اول: استفاده از `merge`](#merge-lambda)
    - [نسخهٔ دوم: استفاده از Method Reference](#merge-method-ref)
- [۲. Method Reference دقیقاً چیست؟](#what-is-method-ref)
- [۳. پنج نوع Method Reference در Java](#five-types)
    - [نوع اول: ارجاع به متد استاتیک](#static-method)
    - [نوع دوم: ارجاع به متد نمونه با گیرندهٔ مشخص](#bound-instance)
    - [نوع سوم: ارجاع به متد نمونه بدون گیرندهٔ مشخص](#unbound-instance)
    - [نوع چهارم: ارجاع به سازندهٔ کلاس](#class-constructor)
    - [نوع پنجم: ارجاع به سازندهٔ آرایه](#array-constructor)
    - [خلاصهٔ پنج نوع](#types-summary)
- [۴. آیا Method Reference همیشه بهتر از Lambda است؟](#not-always-better)
    - [مثال اول: گاهی Lambda واضح‌تر است](#example1)
    - [مثال دوم: `Function.identity()` یا `x -> x`؟](#example2)
- [۵. چه زمانی Lambda از Method Reference بهتر است؟](#when-lambda-better)
- [۶. استفاده از IDE برای تبدیل Lambda به Method Reference](#ide-tools)
- [جمع‌بندی نهایی Item 43](#final-summary)

[بازگشت به بالا](#top)

---

<a id="main-example"></a>
## ۱. مثال اصلی: استفاده از `Integer::sum` به‌جای Lambda

فرض کن یک `Map` داریم که تعداد تکرار هر کلید را نگه می‌دارد:

<div dir="ltr">

```java
Map<String, Integer> map = new HashMap<>();
```
</div>

می‌خواهیم با دریافت یک کلید:
- اگر کلید وجود نداشت، مقدار آن را `1` قرار دهیم
- اگر کلید وجود داشت، مقدار فعلی آن را یک واحد افزایش دهیم

<a id="merge-lambda"></a>
### نسخهٔ اول: استفاده از `merge`

<div dir="ltr">

```java
map.merge(key, 1, (count, incr) -> count + incr);
```
</div>

| پارامتر | نقش |
|---------|-----|
| `key` | کلیدی که می‌خواهیم ثبت کنیم |
| `1` | مقداری که در صورت نبودن کلید استفاده می‌شود |
| `(count, incr) -> count + incr` | تابعی که در صورت وجود مقدار قبلی، مقدار فعلی و مقدار جدید را جمع می‌کند |

<div dir="ltr">

```java
map.merge("Java", 1, (count, incr) -> count + incr);
```
</div>

اگر مقدار قبلی `"Java"` برابر `3` باشد، نتیجه `4` خواهد شد.

این کد کاملاً درست است؛ اما آیا واقعاً لازم است برای جمع دو عدد، یک lambda تعریف کنیم؟

<a id="merge-method-ref"></a>
### نسخهٔ دوم: استفاده از Method Reference

کلاس `Integer` متد استاتیک زیر را دارد:

<div dir="ltr">

```java
Integer.sum(int a, int b)
```
</div>

این متد دقیقاً دو عدد را جمع می‌کند:

<div dir="ltr">

```java
map.merge(key, 1, Integer::sum);
```
</div>

| Lambda | Method Reference |
|--------|------------------|
| `map.merge(key, 1, (count, incr) -> count + incr);` | `map.merge(key, 1, Integer::sum);` |

**چرا نسخهٔ دوم خواناتر است؟** چون به‌جای توضیح دوبارهٔ نحوهٔ جمع‌کردن دو عدد، مستقیماً به متدی اشاره می‌کنیم که این کار را انجام می‌دهد.

> `Integer::sum` یعنی: «از متد `sum` کلاس `Integer` برای پیاده‌سازی این رفتار استفاده کن.»

> **دقت کن:** `Integer::sum` یک فراخوانی مستقیم نیست؛ یک Method Reference است که در این بافت، برای ساخت تابع موردنیاز `merge` استفاده می‌شود.

[بازگشت به بالا](#top)

---

<a id="what-is-method-ref"></a>
## ۲. Method Reference دقیقاً چیست؟

Method Reference راهی برای اشاره به یک متد موجود است، به‌گونه‌ای که بتوان از آن در جایی استفاده کرد که یک Functional Interface موردنیاز است.

<div dir="ltr">

```java
Function<String, Integer> parser = str -> Integer.parseInt(str);
// معادل:
Function<String, Integer> parser = Integer::parseInt;
```
</div>

> **نکتهٔ مهم:** امضای متد ارجاع‌شده باید با Functional Interface موردنظر سازگار باشد. کامپایلر بررسی می‌کند که ورودی‌ها و خروجی متد بتوانند با قرارداد تابعی موردنیاز مطابقت داشته باشند.

به همین دلیل، Method Reference همیشه جایگزین هر lambda دلخواهی نیست؛ زمانی مناسب است که متد موجود بتواند همان رفتار موردنیاز را فراهم کند.

[بازگشت به بالا](#top)

---

<a id="five-types"></a>
## ۳. پنج نوع Method Reference در Java

<a id="static-method"></a>
### نوع اول: ارجاع به متد استاتیک (Static Method Reference)

<div dir="ltr">

```java
Function<String, Integer> parser = Integer::parseInt;
// معادل: str -> Integer.parseInt(str)
```
</div>

مثال‌های دیگر:
- `Integer::sum`
- `Math::abs`
- `String::valueOf`

> **زمان مناسب:** متد موردنظر مستقل از یک نمونهٔ خاص است.

<a id="bound-instance"></a>
### نوع دوم: ارجاع به متد نمونه با گیرندهٔ مشخص (Bound Instance Method Reference)

<div dir="ltr">

```java
Instant instant = Instant.now();
Predicate<Instant> isAfter = instant::isAfter;
// معادل: t -> instant.isAfter(t)
```
</div>

در این مثال، `instant` همان شیئی است که متد `isAfter` روی آن فراخوانی می‌شود.

> **به این دلیل bound می‌گوییم که گیرندهٔ متد (شیء `instant`) از قبل مشخص شده است.**

<a id="unbound-instance"></a>
### نوع سوم: ارجاع به متد نمونه بدون گیرندهٔ مشخص (Unbound Instance Method Reference)

<div dir="ltr">

```java
Function<String, String> lowerCase = String::toLowerCase;
// معادل: str -> str.toLowerCase()
```
</div>

تفاوت با مثال قبلی این است که در اینجا `String::toLowerCase` به یک نمونهٔ خاص از `String` اشاره نمی‌کند. رشته‌ای که متد روی آن اجرا خواهد شد، از طریق آرگومان تابع فراهم می‌شود.

<div dir="ltr">

```java
List<String> names = List.of("ALICE", "BOB", "CHARLIE");

List<String> normalized = names.stream()
    .map(String::toLowerCase)
    .toList();
```
</div>

<a id="class-constructor"></a>
### نوع چهارم: ارجاع به سازندهٔ کلاس (Class Constructor Reference)

<div dir="ltr">

```java
Supplier<TreeMap<String, Integer>> factory = TreeMap::new;
// معادل: () -> new TreeMap<>()
```
</div>

هر بار که `factory.get()` را صدا بزنیم، یک نمونهٔ جدید از `TreeMap` ساخته می‌شود.

> **کاربرد:** الگوهای Factory و زمانی که می‌خواهیم ساخت شیء را به‌صورت یک رفتار منتقل کنیم.

<a id="array-constructor"></a>
### نوع پنجم: ارجاع به سازندهٔ آرایه (Array Constructor Reference)

<div dir="ltr">

```java
IntFunction<int[]> arrayFactory = int[]::new;
// معادل: len -> new int[len]
```
</div>

<div dir="ltr">

```java
int[] numbers = arrayFactory.apply(5);  // آرایهٔ int با پنج خانه
```
</div>

> **کاربرد:** در Stream API، برای مثال زمانی که می‌خواهیم عناصر یک جریان را به آرایه تبدیل کنیم.

<a id="types-summary"></a>
### خلاصهٔ پنج نوع

| نوع | مثال | معادل lambda |
|------|------|--------------|
| متد استاتیک | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| متد نمونه با گیرندهٔ مشخص | `instant::isAfter` | `t -> instant.isAfter(t)` |
| متد نمونه بدون گیرندهٔ مشخص | `String::toLowerCase` | `s -> s.toLowerCase()` |
| سازندهٔ کلاس | `TreeMap::new` | `() -> new TreeMap<>()` |
| سازندهٔ آرایه | `int[]::new` | `len -> new int[len]` |

> **تفاوت مهم نوع دوم و سوم:** در Bound، شیء گیرنده از قبل مشخص است؛ در Unbound، شیء گیرنده از طریق ورودی تابع فراهم می‌شود.

[بازگشت به بالا](#top)

---

<a id="not-always-better"></a>
## ۴. آیا Method Reference همیشه بهتر از Lambda است؟

**خیر.** عنوان این آیتم می‌گوید Method Reference را ترجیح دهید، نه اینکه در همهٔ شرایط الزاماً از آن استفاده کنید.

> **معیار اصلی:** خوانایی و وضوح کد است.

<a id="example1"></a>
### مثال اول: گاهی Lambda واضح‌تر است

فرض کن در کلاسی با نام طولانی `GoshThisClassNameIsHumongous` قرار داریم و می‌خواهیم متد `action()` را اجرا کنیم.

**با Method Reference:**

<div dir="ltr">

```java
service.execute(GoshThisClassNameIsHumongous::action);
```
</div>

**با lambda:**

<div dir="ltr">

```java
service.execute(() -> action());
```
</div>

در این مثال، lambda کوتاه‌تر است؛ چون نیازی به نوشتن نام طولانی کلاس ندارد.

<a id="example2"></a>
### مثال دوم: `Function.identity()` یا `x -> x`؟

<div dir="ltr">

```java
Function<String, String> identity = Function.identity();
// معادل:
Function<String, String> identity = x -> x;
```
</div>

در این مورد، نسخهٔ lambda معمولاً کوتاه‌تر و ساده‌تر است.

> **نتیجه:** نباید صرفاً به دلیل استفاده از API استاندارد، همیشه یک روش را به روش دیگر ترجیح دهیم. باید ببینیم کدام گزینه قصد برنامه‌نویس را بهتر بیان می‌کند.

[بازگشت به بالا](#top)

---

<a id="when-lambda-better"></a>
## ۵. چه زمانی Lambda از Method Reference بهتر است؟

گاهی نام پارامترهای lambda به خوانایی کد کمک می‌کند:

<div dir="ltr">

```java
(a, b) -> compareByPriority(a, b)
```
</div>

اگر نام متد یا زمینهٔ استفاده به‌اندازهٔ کافی گویا نباشد، نام‌گذاری پارامترها می‌تواند مفهوم عملیات را روشن‌تر کند.

### استخراج منطق پیچیده به متد

<div dir="ltr">

```java
items.sort((a, b) -> {
    // Several lines of complex comparison logic
    return compareItems(a, b);
});
```
</div>

پس از استخراج منطق:

<div dir="ltr">

```java
items.sort(MyClass::compareItems);
```
</div>

> **مزیت این روش:** فقط کوتاه‌تر شدن کد نیست؛ متد استخراج‌شده می‌تواند نام معنادار، مستندات و تست‌های مستقل داشته باشد.

### قاعدهٔ عملی

| وضعیت | انتخاب |
|--------|--------|
| Lambda فقط یک متد موجود را فراخوانی می‌کند | Method Reference را بررسی کن |
| Lambda قصد برنامه‌نویس را واضح‌تر بیان می‌کند | همان lambda را نگه دار |
| Lambda پیچیده شده است | منطق را به یک متد نام‌دار استخراج کن |

[بازگشت به بالا](#top)

---

<a id="ide-tools"></a>
## ۶. استفاده از IDE برای تبدیل Lambda به Method Reference

بسیاری از IDEها، از جمله IntelliJ IDEA و Eclipse، پیشنهاد می‌کنند lambdaهایی را که می‌توانند به Method Reference تبدیل شوند، ساده کنیم:

<div dir="ltr">

```java
names.stream()
    .map(name -> name.toLowerCase())
    .toList();
// تبدیل به:
names.stream()
    .map(String::toLowerCase)
    .toList();
```
</div>

> **نکته:** معمولاً پذیرفتن چنین پیشنهادی تصمیم خوبی است؛ اما نباید بدون بررسی، همهٔ پیشنهادها را قبول کنیم. گاهی نسخهٔ lambda خواناتر است یا از نام‌گذاری بهتری برای انتقال مفهوم استفاده می‌کند.

> **IDE ابزار کمکی است، نه جایگزین قضاوت برنامه‌نویس.**

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی Item 43

در این آیتم، نویسنده سه اصل مهم را مطرح می‌کند:

| # | اصل |
|---|-----|
| **۱** | وقتی lambda صرفاً یک متد موجود را فراخوانی می‌کند، Method Reference معمولاً کوتاه‌تر و خواناتر است. مثل `Integer::sum` به‌جای `(a, b) -> a + b` |
| **۲** | Method Reference همیشه انتخاب بهتر نیست. اگر lambda کوتاه‌تر یا گویا‌تر باشد، همان را نگه دار |
| **۳** | اگر lambda پیچیده شده است، منطق آن را به یک متد نام‌دار استخراج کن. این کار خوانایی، مستندسازی و قابلیت تست را بهبود می‌دهد |

### جمع‌بندی پنج نوع Method Reference

| نوع | شکل | معادل lambda |
|------|------|--------------|
| استاتیک | `ClassName::staticMethod` | `args -> ClassName.staticMethod(args)` |
| نمونه با گیرندهٔ مشخص | `instance::method` | `args -> instance.method(args)` |
| نمونه بدون گیرندهٔ مشخص | `ClassName::instanceMethod` | `obj -> obj.instanceMethod()` |
| سازندهٔ کلاس | `ClassName::new` | `args -> new ClassName(args)` |
| سازندهٔ آرایه | `Type[]::new` | `len -> new Type[len]` |

### نکتهٔ پایانی

> **هدف این نیست که کمترین تعداد کاراکتر را بنویسیم؛ هدف این است که کد، رفتار موردنظر را با کمترین پیچیدگی ذهنی به خواننده منتقل کند.**

---

[بازگشت به بالا](#top)

</div>