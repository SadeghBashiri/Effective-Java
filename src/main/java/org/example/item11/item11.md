<div dir="rtl">

<a id="top"></a>

# آیتم ۱۱: همیشه hashCode را وقتی equals را Override می‌کنید، Override کنید (Always override hashCode when you override equals)

به **Item 11** رسیدیم. این یکی از مهم‌ترین آیتم‌های کل کتاب Effective Java است. اگر Item 10 (equals) را قانون اول در تعریف هویت منطقی اشیاء بدانیم، Item 11 قانون دوم است؛ این دو همیشه با هم هستند.

تقریباً تمام Collectionهای مبتنی بر Hash در جاوا (مانند `HashMap`، `HashSet`، `ConcurrentHashMap`) روی همین قرارداد ساخته شده‌اند.

---

## فهرست مطالب

- [ایده اصلی (Core Idea)](#core-idea)
- [ابتدا باید HashMap را بشناسیم](#hashmap-internals)
  - [مرحله اول: hashCode](#step1)
  - [مرحله دوم: equals](#step2)
- [قرارداد hashCode (۳ قانون)](#contract)
  - [قانون اول: Consistency](#rule1)
  - [قانون دوم: Equal Objects → Equal HashCodes](#rule2)
  - [قانون سوم: Unequal Objects → Different HashCodes (ترجیحاً)](#rule3)
- [مشکل واقعی چیست؟](#the-problem)
  - [چرا `null` برمی‌گردد؟](#why-null)
- [بدترین hashCode ممکن](#worst-hashcode)
- [یک hashCode خوب چه ویژگی دارد؟](#good-hashcode)
- [دستورالعمل Joshua Bloch](#algorithm)
- [چرا عدد 31؟](#why-31)
- [Objects.hash()](#objects-hash)
  - [مقایسه روش‌ها](#comparison)
- [HashCode Cache](#hashcode-cache)
- [اشتباه رایج](#common-mistake)
- [ارتباط با Item 10](#connection-to-item10)
- [Production-Grade Example](#production-example)
- [نکات کلیدی Item 11](#key-takeaways)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ایده اصلی (Core Idea)

Joshua Bloch فقط یک جمله می‌خواهد بگوید:

> اگر equals را Override کردی ولی hashCode را Override نکردی، برنامه‌ات از نظر منطقی خراب است.

نه اینکه فقط Performance بد شود. بلکه ممکن است برنامه **کاملاً نتیجه اشتباه بدهد.**

[بازگشت به بالا](#top)

---

<a id="hashmap-internals"></a>
## ابتدا باید HashMap را بشناسیم

فرض کنیم این کلاس را داریم:

<div dir="ltr">

```java
class User {
    String nationalId;
}
```
</div>

اگر از آن به عنوان Key استفاده کنیم:

<div dir="ltr">

```java
Map<User, String> map = new HashMap<>();
```
</div>

داخل HashMap چه اتفاقی می‌افتد؟

تقریباً:

```
                HashMap
                    │
        hashCode()  │
            │       ▼
            ▼   Bucket Number
                │
            equals() check
                │
                ▼
            Final Match
```

یعنی HashMap دو مرحله دارد.

<a id="step1"></a>
### مرحله اول: hashCode()

برای پیدا کردن Bucket:

```
hashCode()
    ↓
7426381
    ↓
Bucket 12
```

<a id="step2"></a>
### مرحله دوم: equals()

داخل Bucket، از `equals()` استفاده می‌شود:

```
Bucket 12
    │
    ├── User A
    ├── User B
    └── User C
```

اگر `hashCode` یکی باشد، آنگاه `equals()` بررسی می‌شود.

### بنابراین

HashMap همیشه:

```
hashCode() → equals()
```

را اجرا می‌کند. نه برعکس.

[بازگشت به بالا](#top)

---

<a id="contract"></a>
## قرارداد hashCode (۳ قانون)

کتاب سه قانون معرفی می‌کند.

<a id="rule1"></a>
### قانون اول: Consistency

در طول عمر شیء، تا زمانی که فیلدهای موثر در `equals` تغییر نکنند، `hashCode` نیز نباید تغییر کند.

مثلاً:

<div dir="ltr">

```java
user.hashCode(); // بار اول
user.hashCode(); // بار دوم
user.hashCode(); // بار سوم
```
</div>

همه باید یک عدد ثابت برگردانند.

<a id="rule2"></a>
### قانون دوم: Equal Objects → Equal HashCodes (مهم‌ترین)

اگر `a.equals(b)` درست باشد، حتماً:

```
a.hashCode() == b.hashCode()
```

نیز باید درست باشد. **این قانون اجباری است.**

<a id="rule3"></a>
### قانون سوم: Unequal Objects → Different HashCodes (ترجیحاً)

اگر `equals() == false` باشد، لزومی ندارد `hashCode` متفاوت باشد. ولی **بهتر است** متفاوت باشد. چرا؟ به خاطر Performance.

[بازگشت به بالا](#top)

---

<a id="the-problem"></a>
## مشکل واقعی چیست؟

فرض کنید کلاس `PhoneNumber` را نوشته‌ایم:

<div dir="ltr">

```java
public class PhoneNumber {
    private final int areaCode;
    private final int prefix;
    private final int line;

    @Override
    public boolean equals(Object o) {
        // ... پیاده‌سازی صحیح
    }
    // اما hashCode را Override نکرده‌ایم!
}
```
</div>

اکنون:

<div dir="ltr">

```java
Map<PhoneNumber, String> map = new HashMap<>();

map.put(
    new PhoneNumber(707, 867, 5309),
    "Jenny"
);
```
</div>

بعد:

<div dir="ltr">

```java
map.get(
    new PhoneNumber(707, 867, 5309)
);
```
</div>

انتظار داریم `"Jenny"` برگردد. اما `null` برمی‌گردد.

<a id="why-null"></a>
### چرا `null` برمی‌گردد؟

دو شیء `PhoneNumber A` و `PhoneNumber B` از نظر `equals` برابرند.

اما `Object.hashCode()` اعداد متفاوت تولید می‌کند:

```
A.hashCode() = 10293
B.hashCode() = 98172817
```

در نتیجه:

- A در Bucket 3 قرار می‌گیرد
- B در Bucket 8 جستجو می‌شود

HashMap اصلاً Bucket صحیح را پیدا نمی‌کند. حتی `equals` را هم صدا نمی‌زند.

### تصویری

```
put()
    ↓
hash = 13
    ↓
Bucket 13
    ↓
Stored
```

بعد:

```
get()
    ↓
hash = 27
    ↓
Bucket 27
    ↓
Nothing
    ↓
null
```

در حالی که `equals` کاملاً صحیح نوشته شده است.

[بازگشت به بالا](#top)

---

<a id="worst-hashcode"></a>
## بدترین hashCode ممکن

کتاب مثال معروفی می‌زند:

<div dir="ltr">

```java
@Override
public int hashCode() {
    return 42;
}
```
</div>

از نظر قرارداد **کاملاً صحیح** است.

چرا؟ زیرا همه اشیاء `42` برمی‌گردانند. پس اگر `equals() == true` باشد، `hashCode` نیز برابر است. قرارداد رعایت شده است.

اما Performance؟ **افتضاح.**

همه چیز داخل یک Bucket می‌رود:

```
Bucket 0
Bucket 1
Bucket 2
Bucket 3
    ↓
    User1
    ↓
    User2
    ↓
    User3
    ↓
    User4
    ↓
    User5
    ↓
    ...
```

در نتیجه `HashMap` تقریباً تبدیل می‌شود به `LinkedList`. زمان دسترسی به جای `O(1)` تقریباً `O(n)` می‌شود.

[بازگشت به بالا](#top)

---

<a id="good-hashcode"></a>
## یک hashCode خوب چه ویژگی دارد؟

دو هدف:

1. **Equal Objects → Same Hash**
2. **Different Objects → Different Hash (تا حد ممکن)**

[بازگشت به بالا](#top)

---

<a id="algorithm"></a>
## دستورالعمل Joshua Bloch

برای هر فیلد مهم:

<div dir="ltr">

```java
result = 31 * result + c
```
</div>

استفاده کن.

مثال:

<div dir="ltr">

```java
@Override
public int hashCode() {
    int result = Integer.hashCode(areaCode);
    result = 31 * result + Integer.hashCode(prefix);
    result = 31 * result + Integer.hashCode(line);
    return result;
}
```
</div>

این دقیقاً همان الگویی است که در کلاس‌های استاندارد جاوا هم زیاد می‌بینید.

[بازگشت به بالا](#top)

---

<a id="why-31"></a>
## چرا عدد 31؟

کتاب دلیل جالبی می‌دهد:

- `31` یک عدد اول (Prime) است
- همچنین `31 * x` توسط JVM تقریباً معادل `(x << 5) - x` بهینه‌سازی می‌شود
- یعنی `32x - x` که بسیار سریع است

البته JVMهای مدرن خودشان این بهینه‌سازی را انجام می‌دهند.

[بازگشت به بالا](#top)

---

<a id="objects-hash"></a>
## Objects.hash()

راه ساده‌تر:

<div dir="ltr">

```java
@Override
public int hashCode() {
    return Objects.hash(areaCode, prefix, line);
}
```
</div>

**مزایا:**
- خواناتر
- کدنویسی کمتر
- احتمال خطای کمتر

**معایب:**
- آرایه موقت ایجاد می‌کند
- برای انواع Primitive عمل Boxing انجام می‌دهد
- در مسیرهای بسیار پرتکرار (Hot Path) کندتر از پیاده‌سازی دستی است

<a id="comparison"></a>
### مقایسه روش‌ها

| روش | خوانایی | Performance | مناسب برای |
|-----|---------|-------------|------------|
| پیاده‌سازی دستی (`31 * result + ...`) | متوسط | ⭐⭐⭐⭐⭐ | کلاس‌های پرتکرار، کتابخانه‌ها، سیستم‌های حساس به کارایی |
| `Objects.hash(...)` | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | بیشتر برنامه‌های معمولی و کدهای تجاری |

[بازگشت به بالا](#top)

---

<a id="hashcode-cache"></a>
## HashCode Cache

اگر کلاس Immutable باشد، می‌توان `hashCode` را Cache کرد.

مثلاً:

<div dir="ltr">

```java
private int hashCode; // مقدار Cache شده

@Override
public int hashCode() {
    int result = hashCode;
    if (result == 0) {
        result = Integer.hashCode(areaCode);
        result = 31 * result + Integer.hashCode(prefix);
        result = 31 * result + Integer.hashCode(line);
        hashCode = result;
    }
    return result;
}
```
</div>

### چرا؟

فرض کنید میلیون‌ها بار `HashMap.get(key)` اجرا می‌شود. اگر هر بار `hashCode` دوباره محاسبه شود، CPU هدر می‌رود.

کلاس‌هایی مانند `String` نیز از همین ایده استفاده می‌کنند و مقدار hash را پس از اولین محاسبه ذخیره می‌کنند.

[بازگشت به بالا](#top)

---

<a id="common-mistake"></a>
## اشتباه رایج

برخی برای سریع‌تر شدن، بعضی فیلدها را حذف می‌کنند.

مثلاً:

<div dir="ltr">

```java
class User {
    String firstName;
    String lastName;
    String nationalId;
}
```
</div>

ولی:

<div dir="ltr">

```java
@Override
public int hashCode() {
    return Objects.hash(firstName); // ❌ فقط firstName
}
```
</div>

اگر `equals` هر سه فیلد را مقایسه کند، این پیاده‌سازی قرارداد را نقض نمی‌کند، اما باعث برخوردهای (Collision) بسیار زیاد می‌شود و کارایی HashMap را به شدت کاهش می‌دهد.

**قانون مهم:** هر فیلدی که در `equals` نقش دارد، باید در `hashCode` نیز نقش داشته باشد.

[بازگشت به بالا](#top)

---

<a id="connection-to-item10"></a>
## ارتباط با Item 10

این دو آیتم همیشه با هم هستند:

```
equals()
    ↓
Identity (منطقی)
```

```
hashCode()
    ↓
Location (Bucket)
```

یا به بیان دیگر:

- `equals` مشخص می‌کند **دو شیء از نظر منطقی یکسان هستند یا نه**
- `hashCode` مشخص می‌کند **شیء ابتدا در کدام Bucket جستجو شود**

اگر این دو با هم سازگار نباشند، ساختارهای مبتنی بر Hash رفتار نادرستی خواهند داشت.

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## Production-Grade Example (Best Practice)
<div dir="ltr">

```java
public final class PhoneNumber {

    private final short areaCode;
    private final short prefix;
    private final short lineNum;

    public PhoneNumber(short areaCode, short prefix, short lineNum) {
        this.areaCode = areaCode;
        this.prefix = prefix;
        this.lineNum = lineNum;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PhoneNumber other)) return false;

        return areaCode == other.areaCode
                && prefix == other.prefix
                && lineNum == other.lineNum;
    }

    @Override
    public int hashCode() {
        int result = Short.hashCode(areaCode);
        result = 31 * result + Short.hashCode(prefix);
        result = 31 * result + Short.hashCode(lineNum);
        return result;
    }
}
```
</div>
این پیاده‌سازی دقیقاً با توصیه‌های کتاب سازگار است و برای استفاده در `HashMap`، `HashSet` و سایر ساختارهای مبتنی بر Hash مناسب است.

[بازگشت به بالا](#top)

---

<a id="key-takeaways"></a>
## نکات کلیدی Item 11

| نکته | توضیح |
|------|-------|
| **قانون طلایی** | هر زمان `equals()` را Override می‌کنید، **باید** `hashCode()` را نیز Override کنید |
| **شرط لازم** | اشیای برابر از نظر `equals` **حتماً** باید `hashCode` یکسان داشته باشند |
| **توزیع مناسب** | یک `hashCode` خوب، اشیای نابرابر را تا حد امکان روی Bucketهای مختلف توزیع می‌کند |
| **انتخاب روش** | برای اغلب پروژه‌ها، `Objects.hash(...)` کافی است؛ اما در کتابخانه‌ها یا مسیرهای حساس به کارایی، پیاده‌سازی دستی معمولاً بهتر است |
| **Cache کردن** | برای کلاس‌های Immutable که به‌طور گسترده به عنوان کلید استفاده می‌شوند، Cache کردن مقدار `hashCode` می‌تواند سودمند باشد |
| **فیلدهای مشارکت‌کننده** | هر فیلدی که در `equals` مشارکت دارد، باید در `hashCode` نیز مشارکت داشته باشد؛ در غیر این صورت رفتار Collectionهای مبتنی بر Hash قابل اعتماد نخواهد بود |

### خلاصه نهایی

```
اگر equals را Override کردی:
    1. همیشه hashCode را نیز Override کن
    2. از فیلدهای یکسان در هر دو استفاده کن
    3. توزیع مناسبی را تضمین کن
    4. برای Performance، Cache کردن را در نظر بگیر
```

[بازگشت به بالا](#top)

</div>
```