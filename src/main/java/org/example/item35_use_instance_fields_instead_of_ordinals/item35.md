<div dir="rtl">

<a id="top"></a>

# آیتم ۳۵: به‌جای `ordinal()` از فیلد نمونه استفاده کنید

## (Use instance fields instead of ordinals)

این Item در واقع ادامه‌ی مستقیم Item 34 است. در Item 34 یاد گرفتیم که **Enum یک کلاس واقعی است و می‌تواند state و behavior داشته باشد**. در Item 35، کتاب روی یک اشتباه بسیار رایج تمرکز می‌کند:

> **هرگز از موقعیت (`ordinal`) یک enum constant به‌عنوان مقدار معنایی آن استفاده نکنید.**

یعنی اگر یک Enum دارای یک عدد مرتبط با هر عضو است، آن عدد را **صریحاً داخل خود constant ذخیره کنید**، نه اینکه از جایگاه constant در Enum آن را استخراج کنید.

---

## فهرست مطالب

- [۱. اول `ordinal()` دقیقاً چیست؟](#ordinal-definition)
- [۲. Anti-Pattern کتاب](#anti-pattern-book)
- [۳. چرا این کار Maintenance Nightmare است؟](#maintenance)
- [۴. مشکل عمیق‌تر: Declaration Order تبدیل به Business Rule شده](#deeper-problem)
- [۵. مشکل دوم: دو Constant با یک مقدار](#duplicate-values)
- [۶. مشکل سوم: مقدارهای Missing](#missing-values)
- [۷. راه‌حل صحیح: Instance Field](#solution)
- [۸. تفاوت معماری این دو طراحی](#architectural-difference)
- [۹. چرا `final`؟](#why-final)
- [۱۰. نکته مهم: Enum Constant واقعاً Object است](#constant-is-object)
- [۱۱. `ordinal()` پس برای چه ساخته شده؟](#why-ordinal)
- [۱۲. `ordinal()` را با ID اشتباه نگیریم](#ordinal-vs-id)
- [۱۳. Production-Grade Design](#production-design)
- [۱۴. حتی بهتر: `fromCode()`](#fromcode)
- [۱۵. سه مفهوم را کاملاً از هم جدا کن](#three-concepts)
- [۱۶. `ordinal()` حتی برای نمایش هم مناسب نیست](#not-for-display)
- [۱۷. Anti-Patternهای مهم Item 35](#anti-patterns)
- [۱۸. نکته Production مهم‌تر: حتی `name()` هم همیشه مناسب نیست](#name-not-always)
- [۱۹. Enum + Database](#enum-database)
- [۲۰. رابطه Item 34 و Item 35](#relation-34-35)
- [۲۱. نگاه معماری: `ordinal` یک Implementation Detail است](#architectural-view)
- [۲۲. آیا `ordinal()` هیچ‌وقت در کد Application استفاده شود؟](#when-to-use-ordinal)
- [۲۳. یک اشتباه ظریف: استفاده از ordinal برای Array Index](#array-index-mistake)
- [۲۴. یک مثال Production-Grade بهتر](#production-example)
- [۲۵. Decision Matrix](#decision-matrix)
- [۲۶. یک نکته بسیار مهم: Enum برای مقادیر Dynamic نیست](#not-dynamic)
- [۲۷. خلاصه‌ی Senior-Level Item 35](#senior-summary)
- [ارتباط Itemهای 34 و 35 در یک تصویر ذهنی](#mental-model)

[بازگشت به بالا](#top)

---

<a id="ordinal-definition"></a>
## ۱. اول `ordinal()` دقیقاً چیست؟

فرض کنید:

<div dir="ltr">

```java
public enum Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY
}
```
</div>

هر enum constant یک موقعیت دارد:

<div dir="ltr">

```text
MONDAY     → ordinal = 0
TUESDAY    → ordinal = 1
WEDNESDAY  → ordinal = 2
THURSDAY   → ordinal = 3
FRIDAY     → ordinal = 4
```
</div>

پس:

<div dir="ltr">

```java
Day.MONDAY.ordinal();    // 0
Day.TUESDAY.ordinal();   // 1
Day.FRIDAY.ordinal();    // 4
```
</div>

### `ordinal()` معنای business ندارد.

فقط می‌گوید:

> «این constant چندمین constant در declaration این enum است؟»

[بازگشت به بالا](#top)

---

<a id="anti-pattern-book"></a>
## ۲. Anti-Pattern کتاب

کتاب مثال بسیار خوبی دارد:

<div dir="ltr">

```java
public enum Ensemble {
    SOLO,
    DUET,
    TRIO,
    QUARTET,
    QUINTET,
    SEXTET,
    SEPTET,
    OCTET,
    NONET,
    DECTET;

    public int numberOfMusicians() {
        return ordinal() + 1;
    }
}
```
</div>

در نگاه اول کاملاً منطقی به نظر می‌رسد:

<div dir="ltr">

```java
Ensemble.SOLO.numberOfMusicians();      // 1
Ensemble.DUET.numberOfMusicians();      // 2
Ensemble.TRIO.numberOfMusicians();      // 3
Ensemble.OCTET.numberOfMusicians();     // 8
```
</div>

اما این کد یک coupling بسیار خطرناک ایجاد کرده است:

<div dir="ltr">

```text
Business Meaning → ordinal() → Declaration Order
```
</div>

یعنی: تعداد نوازندگان مستقیماً به ترتیب قرار گرفتن constantها وابسته شده است. و این همان چیزی است که نباید اتفاق بیفتد.

[بازگشت به بالا](#top)

---

<a id="maintenance"></a>
## ۳. چرا این کار Maintenance Nightmare است؟

### مشکل اول: Reordering

فرض کنید بعداً کسی Enum را این‌طور مرتب کند:

<div dir="ltr">

```java
public enum Ensemble {
    SOLO,
    DUET,
    TRIO,
    QUARTET,
    OCTET,
    QUINTET,
    SEXTET,
    SEPTET,
    NONET,
    DECTET;
}
```
</div>

حالا:

<div dir="ltr">

```text
SOLO      → 0 + 1 = 1
DUET      → 1 + 1 = 2
TRIO      → 2 + 1 = 3
QUARTET   → 3 + 1 = 4
OCTET     → 4 + 1 = 5  ❌
```
</div>

در حالی که `OCTET = 8 musicians` است.

یعنی صرفاً با **مرتب کردن declaration**، منطق business خراب شد.

[بازگشت به بالا](#top)

---

<a id="deeper-problem"></a>
## ۴. مشکل عمیق‌تر: Declaration Order تبدیل به Business Rule شده

این قسمت از نظر معماری بسیار مهم است.

ما دو مفهوم کاملاً متفاوت داریم:

- **`ordinal()`**: جایگاه این constant در declaration
- **`numberOfMusicians`**: یک business property

این دو هیچ ارتباط ذاتی‌ای با هم ندارند.

اما Anti-Pattern آنها را به هم وصل کرده:

<div dir="ltr">

```text
ordinal → numberOfMusicians
```
</div>

در حالی که باید این‌گونه باشد:

<div dir="ltr">

```text
Ensemble
   ├── name
   └── numberOfMusicians
```
</div>

یعنی مقدار business باید **جزئی از state خود object باشد.**

[بازگشت به بالا](#top)

---

<a id="duplicate-values"></a>
## ۵. مشکل دوم: دو Constant با یک مقدار

فرض کنید می‌خواهیم:

<div dir="ltr">

```text
OCTET = 8
DOUBLE_QUARTET = 8
```
</div>

داشته باشیم.

با ordinal غیرممکن است. چرا؟ چون `OCTET.ordinal()` و `DOUBLE_QUARTET.ordinal()` نمی‌توانند هر دو یک مقدار باشند. هر constant باید یک position متفاوت داشته باشد.

مثلاً:

<div dir="ltr">

```text
OCTET           → ordinal 7
DOUBLE_QUARTET  → ordinal 8
```
</div>

پس `ordinal() + 1` به ترتیب `8` و `9` خواهد شد، در حالی که business می‌گوید هر دو `8`.

> `ordinal` نمی‌تواند representation مناسبی برای یک property مستقل از ترتیب باشد.

[بازگشت به بالا](#top)

---

<a id="missing-values"></a>
## ۶. مشکل سوم: مقدارهای Missing

حالا فرض کنید `TRIPLE_QUARTET = 12` داریم. ولی `11 musicians` مقدار معنایی خاصی ندارد.

با ordinal نمی‌توانیم مستقیماً بگوییم `TRIPLE_QUARTET → 12` مگر اینکه constantهای قبل از آن را طوری بچینیم که ordinal به 11 برسد. یعنی ممکن است مجبور شویم چیزی شبیه این بسازیم:

<div dir="ltr">

```java
public enum Ensemble {
    SOLO, DUET, TRIO, QUARTET, QUINTET,
    SEXTET, SEPTET, OCTET, NONET, DECTET,
    UNUSED_11,
    TRIPLE_QUARTET
}
```
</div>

که واقعاً فاجعه است. یک constant مصنوعی فقط به خاطر اینکه ordinal به عدد موردنظر برسد!

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## ۷. راه‌حل صحیح: Instance Field

راه‌حل بسیار ساده است:

<div dir="ltr">

```java
public enum Ensemble {

    SOLO(1),
    DUET(2),
    TRIO(3),
    QUARTET(4),
    QUINTET(5),
    SEXTET(6),
    SEPTET(7),
    OCTET(8),
    DOUBLE_QUARTET(8),
    NONET(9),
    DECTET(10),
    TRIPLE_QUARTET(12);

    private final int numberOfMusicians;

    Ensemble(int numberOfMusicians) {
        this.numberOfMusicians = numberOfMusicians;
    }

    public int numberOfMusicians() {
        return numberOfMusicians;
    }
}
```
</div>

حالا:

<div dir="ltr">

```java
Ensemble.OCTET.numberOfMusicians();            // 8
Ensemble.DOUBLE_QUARTET.numberOfMusicians();   // 8
Ensemble.TRIPLE_QUARTET.numberOfMusicians();   // 12
```
</div>

بدون اینکه هیچ ارتباطی با `ordinal()` داشته باشد.

[بازگشت به بالا](#top)

---

<a id="architectural-difference"></a>
## ۸. تفاوت معماری این دو طراحی

### Anti-Pattern

<div dir="ltr">

```java
public int numberOfMusicians() {
    return ordinal() + 1;
}
```
</div>

مدل ذهنی:

<div dir="ltr">

```text
Constant → Position → Business Value
```
</div>

### Best Practice

<div dir="ltr">

```java
private final int numberOfMusicians;
```
</div>

مدل ذهنی:

<div dir="ltr">

```text
Constant
   ├── ordinal → technical metadata
   └── numberOfMusicians → business data
```
</div>

این تفکیک بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="why-final"></a>
## ۹. چرا `final`؟

چون enum constant باید immutable باشد.

`Ensemble.OCTET` یک object مشخص است که باید همیشه `numberOfMusicians = 8` داشته باشد.

نباید بتوانیم `octet.setNumberOfMusicians(15)` انجام دهیم.

بنابراین:

<div dir="ltr">

```java
private final int numberOfMusicians;
```
</div>

به خوبی invariant را حفظ می‌کند.

[بازگشت به بالا](#top)

---

<a id="constant-is-object"></a>
## ۱۰. نکته مهم: Enum Constant واقعاً Object است

این Item بدون درک این موضوع کامل فهمیده نمی‌شود.

وقتی می‌نویسیم:

<div dir="ltr">

```java
public enum Ensemble {
    SOLO(1), DUET(2), TRIO(3);

    private final int numberOfMusicians;

    Ensemble(int numberOfMusicians) {
        this.numberOfMusicians = numberOfMusicians;
    }
}
```
</div>

در واقع هر constant یک instance از `Ensemble` است:

<div dir="ltr">

```text
Ensemble
   ├── SOLO
   │     └── numberOfMusicians = 1
   ├── DUET
   │     └── numberOfMusicians = 2
   └── TRIO
         └── numberOfMusicians = 3
```
</div>

بنابراین کاملاً طبیعی است که هر constant state خودش را داشته باشد.

[بازگشت به بالا](#top)

---

<a id="why-ordinal"></a>
## ۱۱. `ordinal()` پس برای چه ساخته شده؟

> `ordinal()` عمدتاً برای general-purpose enum-based data structures طراحی شده است.

یعنی مواردی مثل `EnumSet` و `EnumMap`.

در implementationهای داخلی می‌توانند از موقعیت enum constant استفاده کنند. مثلاً:

<div dir="ltr">

```java
enum Color { RED, GREEN, BLUE }
```
</div>

از دید implementation:

<div dir="ltr">

```text
RED   → 0
GREEN → 1
BLUE  → 2
```
</div>

می‌تواند برای ساختارهای داده‌ای specialized مفید باشد. اما این یک استفاده‌ی **technical/internal** است، نه business-level.

[بازگشت به بالا](#top)

---

<a id="ordinal-vs-id"></a>
## ۱۲. `ordinal()` را با ID اشتباه نگیریم

<div dir="ltr">

```java
public enum Status {
    PENDING,
    PAID,
    CANCELLED
}
```
</div>

و بعد `status.ordinal()` را در database ذخیره کند:

<div dir="ltr">

```text
PENDING   → 0
PAID      → 1
CANCELLED → 2
```
</div>

### ❌ این کار را نکنید.

فرض کنید بعداً:

<div dir="ltr">

```java
public enum Status {
    PENDING,
    FAILED,
    PAID,
    CANCELLED
}
```
</div>

شود. حالا:

<div dir="ltr">

```text
PENDING   → 0
FAILED    → 1
PAID      → 2
CANCELLED → 3
```
</div>

و داده‌های قبلی `1` که به معنی `PAID` بودند، اکنون تبدیل می‌شوند به `FAILED`. این یک **Data Corruption Bug** است.

[بازگشت به بالا](#top)

---

<a id="production-design"></a>
## ۱۳. Production-Grade Design

اگر Enum یک business code دارد، آن را صریحاً تعریف کنید:

<div dir="ltr">

```java
public enum OrderStatus {

    PENDING("P"),
    PAID("PA"),
    CANCELLED("C"),
    FAILED("F");

    private final String code;

    OrderStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
```
</div>

حالا:

<div dir="ltr">

```java
OrderStatus.PAID.code()   // → "PA"
```
</div>

این مقدار مستقل از ترتیب Enum است.

[بازگشت به بالا](#top)

---

<a id="fromcode"></a>
## ۱۴. حتی بهتر: `fromCode()`

در سیستم‌های واقعی معمولاً فقط getter کافی نیست.

<div dir="ltr">

```java
public enum OrderStatus {

    PENDING("P"),
    PAID("PA"),
    CANCELLED("C"),
    FAILED("F");

    private final String code;

    OrderStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<OrderStatus> fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst();
    }
}
```
</div>

استفاده:

<div dir="ltr">

```java
Optional<OrderStatus> status = OrderStatus.fromCode("PA");
// → Optional[PAID]
```
</div>

در پروژه‌های بزرگ‌تر، اگر lookup زیاد باشد، بهتر است mapping را یک بار بسازیم:

<div dir="ltr">

```java
private static final Map<String, OrderStatus> BY_CODE =
        Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(
                        OrderStatus::code,
                        Function.identity()
                ));

public static Optional<OrderStatus> fromCode(String code) {
    return Optional.ofNullable(BY_CODE.get(code));
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="three-concepts"></a>
## ۱۵. سه مفهوم را کاملاً از هم جدا کن

| مفهوم | معنی | مناسب برای Business؟ |
|--------|------|---------------------:|
| `ordinal()` | جایگاه constant | ❌ |
| `name()` | نام Java constant | ⚠️ |
| instance field مثل `code` | مقدار معنایی تعریف‌شده | ✅ |

<div dir="ltr">

```text
PENDING
   ├── ordinal() = 0          ← technical position
   ├── name()    = "PENDING"  ← Java identifier
   └── code      = "P"        ← business/external value
```
</div>

این سه تا را نباید یکی فرض کرد.

[بازگشت به بالا](#top)

---

<a id="not-for-display"></a>
## ۱۶. `ordinal()` حتی برای نمایش هم مناسب نیست

<div dir="ltr">

```java
System.out.println(status.ordinal());  // اطلاعات بسیار ضعیفی می‌دهد: 2
System.out.println(status);            // بهتر: FAILED
status.code()                          // دقیق‌تر: F
```
</div>

هر کدام semantics متفاوتی دارند.

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## ۱۷. Anti-Patternهای مهم Item 35

### ❌ Anti-Pattern 1 — استخراج Business Value

<div dir="ltr">

```java
public enum Priority {
    LOW, MEDIUM, HIGH;

    public int level() {
        return ordinal();
    }
}
```
</div>

بهتر:

<div dir="ltr">

```java
public enum Priority {
    LOW(10), MEDIUM(50), HIGH(100);

    private final int level;

    Priority(int level) { this.level = level; }
    public int level() { return level; }
}
```
</div>

### ❌ Anti-Pattern 2 — Database ID

<div dir="ltr">

```java
repository.save(status.ordinal());
```
</div>

### ❌ Anti-Pattern 3 — API code

<div dir="ltr">

```java
response.setStatus(status.ordinal());
```
</div>

### ❌ Anti-Pattern 4 — Serialization

<div dir="ltr">

```java
json.put("status", status.ordinal());
```
</div>

این‌ها همه coupling بین **external representation** و **declaration order** ایجاد می‌کنند.

[بازگشت به بالا](#top)

---

<a id="name-not-always"></a>
## ۱۸. نکته Production مهم‌تر: حتی `name()` هم همیشه مناسب نیست

ممکن است کسی بگوید: "خب `ordinal()` بد است؛ پس `name()` را ذخیره کنیم."

<div dir="ltr">

```java
status.name()   // → "PAID"
```
</div>

از ordinal بهتر است، اما هنوز یک مشکل دارد.

اگر بعداً `PAID` را به `COMPLETED` تغییر دهید، contract خارجی شکسته می‌شود.

برای سیستم‌های واقعی بهتر است:

<div dir="ltr">

```java
PAID("PA")
```
</div>

داشته باشیم.

<div dir="ltr">

```text
Java identifier ≠ External contract
```
</div>

[بازگشت به بالا](#top)

---

<a id="enum-database"></a>
## ۱۹. Enum + Database

<div dir="ltr">

```java
public enum PaymentStatus {

    PENDING("P"),
    AUTHORIZED("A"),
    CAPTURED("C"),
    FAILED("F");

    private final String code;

    PaymentStatus(String code) { this.code = code; }
    public String code() { return code; }
}
```
</div>

Database:

<div dir="ltr">

```text
payment_status
--------------
P
A
C
F
```
</div>

نه:

<div dir="ltr">

```text
0
1
2
3
```
</div>

چون:

<div dir="ltr">

```text
ordinal → implementation detail
code → explicit domain contract
```
</div>

در JPA/Hibernate هم اگر از enum persistence استفاده می‌کنید، برای domainهای حساس معمولاً باید با دقت تصمیم بگیرید که `STRING` کافی است یا explicit converter برای `code` بهتر است؛ **هرگز `ORDINAL` را صرفاً به‌خاطر راحتی انتخاب نکنید.**

[بازگشت به بالا](#top)

---

<a id="relation-34-35"></a>
## ۲۰. رابطه Item 34 و Item 35

### Item 34

می‌گوید: به جای `int` constants از Enum استفاده کن.

<div dir="ltr">

```java
public static final int PAID = 1;   // ❌
```
</div>

تبدیل شود به:

<div dir="ltr">

```java
enum PaymentStatus { PAID }
```
</div>

### Item 35

حالا می‌گوید: اگر Enum یک مقدار عددی/رشته‌ای/کدی دارد، آن را از `ordinal()` استخراج نکن.

<div dir="ltr">

```java
enum PaymentStatus {
    PENDING, PAID, FAILED;

    int code() {
        return ordinal();  // ❌
    }
}
```
</div>

بلکه:

<div dir="ltr">

```java
enum PaymentStatus {
    PENDING(10), PAID(20), FAILED(30);

    private final int code;

    PaymentStatus(int code) { this.code = code; }
}
```
</div>

پس:

<div dir="ltr">

```text
Item 34 → Use Enum instead of constants
Item 35 → Give Enum explicit state
        → Do NOT derive domain state from ordinal
```
</div>

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## ۲۱. نگاه معماری: `ordinal` یک Implementation Detail است

<div dir="ltr">

```text
ordinal() → Implementation Detail
code / id / priority / weight / timeout / displayName / externalCode → Domain Data
```
</div>

نباید این دو لایه را به هم متصل کنیم.

<div dir="ltr">

```text
Domain → ordinal → Java declaration order    // ❌ leaky abstraction
Domain → explicit field                       // ✅ درست
```
</div>

[بازگشت به بالا](#top)

---

<a id="when-to-use-ordinal"></a>
## ۲۲. آیا `ordinal()` هیچ‌وقت در کد Application استفاده شود؟

> اگر دلیل مشخصی ندارید، اصلاً از `ordinal()` استفاده نکنید.

اگر دیدید:

<div dir="ltr">

```java
something.ordinal()
```
</div>

در Production Code، فوراً سؤال کنید: "چرا؟"

اگر پاسخ این بود: "برای اینکه شماره این enum را بگیریم." احتمال زیادی دارد که طراحی اشتباه باشد.

اما اگر در حال پیاده‌سازی یک **general-purpose enum-based data structure** هستید، قضیه متفاوت است. این همان جایی است که `EnumSet` و `EnumMap` اهمیت پیدا می‌کنند.

[بازگشت به بالا](#top)

---

<a id="array-index-mistake"></a>
## ۲۳. یک اشتباه ظریف: استفاده از ordinal برای Array Index

<div dir="ltr">

```java
private final String[] messages = {
    "Pending", "Paid", "Failed"
};

String message(PaymentStatus status) {
    return messages[status.ordinal()];
}
```
</div>

اگر:

<div dir="ltr">

```java
enum PaymentStatus {
    FAILED, PENDING, PAID
}
```
</div>

شود:

<div dir="ltr">

```text
FAILED  → "Pending" ❌
PENDING → "Paid"    ❌
PAID    → "Failed"  ❌
```
</div>

اگر واقعاً mapping لازم دارید، بهتر است خود mapping را explicit کنید:

<div dir="ltr">

```java
private static final Map<PaymentStatus, String> MESSAGES =
        Map.of(
            PaymentStatus.PENDING, "Pending",
            PaymentStatus.PAID, "Paid",
            PaymentStatus.FAILED, "Failed"
        );
```
</div>

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## ۲۴. یک مثال Production-Grade بهتر

<div dir="ltr">

```java
public enum OrderStatus {

    PENDING("P", false),
    CONFIRMED("C", false),
    SHIPPED("S", false),
    DELIVERED("D", true),
    CANCELLED("X", true);

    private final String code;
    private final boolean terminal;

    OrderStatus(String code, boolean terminal) {
        this.code = code;
        this.terminal = terminal;
    }

    public String code() { return code; }
    public boolean isTerminal() { return terminal; }
}
```
</div>

حالا:

<div dir="ltr">

```java
OrderStatus.SHIPPED.isTerminal();    // false
OrderStatus.DELIVERED.isTerminal();  // true
```
</div>

قدرت Enum:

<div dir="ltr">

```text
Enum Constant
      ├── identity
      ├── explicit data
      └── behavior
```
</div>

و هیچ‌کدام وابسته به `ordinal()` نیستند.

[بازگشت به بالا](#top)

---

<a id="decision-matrix"></a>
## ۲۵. Decision Matrix

| راهکار | Type Safety | قابل تغییر بودن ترتیب | External Contract | مناسب Production |
|--------|------------:|----------------------:|------------------:|-----------------:|
| `int constant` | ❌ | ❌ | ضعیف | ❌ |
| `String constant` | ❌ | ✅ | متوسط | ⚠️ |
| `enum + ordinal()` | ✅ | ❌ | ❌ | ❌ |
| `enum + name()` | ✅ | ✅ | ⚠️ | ⚠️ |
| `enum + explicit field` | ✅ | ✅ | ✅ | ✅ |
| Dynamic Value Object | ✅ | ✅ | ✅ | ✅ برای مقادیر dynamic |

[بازگشت به بالا](#top)

---

<a id="not-dynamic"></a>
## ۲۶. یک نکته بسیار مهم: Enum برای مقادیر Dynamic نیست

Item 35 نباید باعث شود فکر کنیم: "پس همیشه برای هر code از enum استفاده کنیم."

خیر. Enum زمانی مناسب است که مجموعه‌ی مقادیر **در زمان compile شناخته‌شده و نسبتاً closed** باشد.

مثلاً:

<div dir="ltr">

```java
PaymentStatus / OrderStatus / DayOfWeek / Currency / RoundingMode
```
</div>

اما اگر مقادیر توسط database اضافه می‌شوند:

<div dir="ltr">

```text
Tenant-defined roles
Dynamic product categories
Admin-defined statuses
Plugin-defined payment methods
```
</div>

Enum انتخاب مناسبی نیست.

در چنین مواردی بهتر است از چیزی مانند:

<div dir="ltr">

```java
public record StatusCode(String value) {}
```
</div>

یا Entity / Value Object استفاده کنیم.

[بازگشت به بالا](#top)

---

<a id="senior-summary"></a>
## ۲۷. خلاصه‌ی Senior-Level Item 35

| Rule | توضیح |
|------|-------|
| **Rule 1** | `enum.ordinal()` یعنی **position**، نه **business value** |
| **Rule 2** | اگر Enum یک مقدار مرتبط دارد: `private final int value;` یا `private final String code;` تعریف کن |
| **Rule 3** | هیچ‌وقت برای persistence `status.ordinal()` را ذخیره نکن |
| **Rule 4** | برای API و Message Contract، مقدار explicit تعریف کن: `PAID("PA")`، نه `PAID.ordinal()` |
| **Rule 5** | ترتیب constantها باید بتواند آزادانه تغییر کند، بدون اینکه business logic خراب شود |
| **Rule 6** | اگر دو constant باید یک مقدار داشته باشند، instance field به‌راحتی این امکان را می‌دهد: `OCTET(8)`, `DOUBLE_QUARTET(8)` |
| **Rule 7** | `ordinal()` را عمدتاً برای infrastructureهای generic مانند `EnumSet` و `EnumMap` در نظر بگیر، نه business logic |

[بازگشت به بالا](#top)

---

<a id="mental-model"></a>
## ارتباط Itemهای 34 و 35 در یک تصویر ذهنی

<div dir="ltr">

<div dir="ltr">

```text
                 ENUM
                   │
          ┌────────┴────────┐
          │                 │
     Identity           Explicit State
          │                 │
     PAID                code = "PA"
     FAILED              priority = 100
     PENDING             retryable = true
          │                 │
          │                 │
      ordinal()          instance fields
          │                 │
   technical detail     domain meaning
          │                 │
          └───────┬─────────┘
                  │
          Business should
          depend on explicit
          domain state
```
</div>
</div>

### جوهر Item 35

> `ordinal()` به شما می‌گوید یک Enum constant **کجای لیست declaration قرار گرفته است**؛ نمی‌گوید آن constant **چه معنایی در Domain دارد**.

بنابراین هر وقت یک Enum property واقعی دارد، آن property را **explicitly در یک `final` instance field نگهداری کن**. این کار Enum را در برابر reorder، حذف/اضافه شدن constantها، duplicate values و evolution آینده مقاوم می‌کند.

---

[بازگشت به بالا](#top)

</div>
