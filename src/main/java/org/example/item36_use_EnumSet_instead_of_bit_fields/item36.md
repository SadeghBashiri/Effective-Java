<div dir="rtl">

<a id="top"></a>

# آیتم ۳۶: به‌جای Bit Field از `EnumSet` استفاده کنید

## (Use `EnumSet` instead of bit fields)

این Item یکی از نمونه‌های خیلی خوب در Effective Java است که نشان می‌دهد یک نکته‌ی مهم در طراحی API چیست:

> **اگر مجموعه‌ای از مقادیر یک `enum` دارید، آن مجموعه را با `Set<Enum>` مدل کنید، نه با یک `int` یا `long` که با Bitwise Operation چند مقدار را داخل آن فشرده کرده‌اید.**

نکته‌ی بسیار جالب اینجاست که `EnumSet` در داخل خودش از همان تکنیک Bitwise استفاده می‌کند؛ بنابراین شما **سادگی و Type Safety یک Set را می‌گیرید، ولی Performance مدل Bit Field را هم تا حد زیادی حفظ می‌کنید.**

---

## فهرست مطالب

- [۱. مسئله از کجا شروع می‌شود؟](#problem)
- [۲. راهکار قدیمی: Bit Field](#bit-field)
- [۳. چگونه چند Style را با هم ترکیب کنیم؟](#combine)
- [۴. چرا Bit Field از نظر Performance جذاب بود؟](#performance-appeal)
- [۵. پس مشکل چیست؟](#the-problem)
- [۶. مشکل حتی بدتر از Item 34 است](#worse-than-item34)
- [۷. مشکل دوم: Iteration](#iteration-problem)
- [۸. مشکل سوم: ظرفیت محدود](#capacity-limit)
- [۹. این دقیقاً چیزی است که Effective Java می‌خواهد از آن جلوگیری کند](#abstraction)
- [۱۰. راه‌حل مدرن: Enum + EnumSet](#modern-solution)
- [۱۱. API را چگونه طراحی کنیم؟](#api-design)
- [۱۲. Type Safety چه تغییری کرده؟](#type-safety-change)
- [۱۳. EnumSet دقیقاً چیست؟](#what-is-enumset)
- [۱۴. قسمت بسیار جالب: EnumSet در داخل تقریباً همان Bit Field است!](#internal-bitfield)
- [۱۵. یعنی `EnumSet` در واقع این فلسفه را دارد](#enumset-philosophy)
- [۱۶. مقایسه مستقیم](#direct-comparison)
- [۱۷. عملیات Set چگونه تبدیل می‌شوند؟](#set-operations)
- [۱۸. چرا `Set<Style>` و نه `EnumSet<Style>`؟](#set-vs-enumset)
- [۱۹. اصل مهم API Design](#api-design-principle)
- [۲۰. اما اگر همه EnumSet بدهند چه؟](#enumset-passing)
- [۲۱. Factoryهای بسیار خوب EnumSet](#factories)
- [۲۲. Iteration چقدر بهتر می‌شود؟](#iteration-better)
- [۲۳. یک Production Example](#production-example)
- [۲۴. Anti-Pattern همین مثال](#anti-pattern-example)
- [۲۵. نکته مهم درباره `EnumSet` و `null`](#enumset-null)
- [۲۶. EnumSet برای یک Enum خاص است](#enumset-specific)
- [۲۷. Bit Field حتی می‌تواند APIهای خطرناک ایجاد کند](#dangerous-api)
- [۲۸. یک نکته مهم Performance](#performance-note)
- [۲۹. EnumSet در برابر HashSet](#enumset-vs-hashset)
- [۳۰. یک نکته ظریف درباره ظرفیت EnumSet](#capacity-note)
- [۳۱. یک نکته معماری بسیار مهم: Representation Independence](#representation-independence)
- [۳۲. Immutable EnumSet چه؟](#immutable-enumset)
- [۳۳. آیا `Set.of()` جایگزین EnumSet است؟](#set-of)
- [۳۴. ارتباط Item 34 → 35 → 36](#chain-34-35-36)
- [۳۵. و این زنجیره از نظر Type System خیلی مهم است](#type-system-chain)
- [۳۶. Anti-Pattern vs Best Practice](#anti-vs-best)
- [۳۷. Production-Grade Version](#production-grade)
- [۳۸. چه زمانی Bit Field هنوز قابل قبول است؟](#when-bitfield-ok)
- [۳۹. Decision Framework](#decision-framework)
- [۴۰. خلاصه نهایی Item 36](#final-summary)

[بازگشت به بالا](#top)

---

<a id="problem"></a>
## ۱. مسئله از کجا شروع می‌شود؟

فرض کنید یک Text Editor داریم و می‌خواهیم Styleهای مختلف متن را مشخص کنیم:

<div dir="ltr">

```text
BOLD
ITALIC
UNDERLINE
STRIKETHROUGH
```
</div>

مثلاً یک متن می‌تواند همزمان `BOLD + ITALIC` باشد. یا `BOLD + UNDERLINE + ITALIC`.

پس ما در واقع با یک **Set از Styleها** مواجه هستیم.

از نظر Domain:

<div dir="ltr">

```text
Text
 └── styles
      ├── BOLD
      ├── ITALIC
      └── UNDERLINE
```
</div>

[بازگشت به بالا](#top)

---

<a id="bit-field"></a>
## ۲. راهکار قدیمی: Bit Field

قبل از اینکه Enum و `EnumSet` وجود داشته باشند، یک تکنیک رایج استفاده از بیت‌های یک `int` بود:

<div dir="ltr">

```java
public class Text {

    public static final int STYLE_BOLD          = 1 << 0; // 0001 = 1
    public static final int STYLE_ITALIC        = 1 << 1; // 0010 = 2
    public static final int STYLE_UNDERLINE     = 1 << 2; // 0100 = 4
    public static final int STYLE_STRIKETHROUGH = 1 << 3; // 1000 = 8

    public void applyStyles(int styles) {
        // ...
    }
}
```
</div>

هر Style یک bit مخصوص خودش دارد:

<div dir="ltr">

```text
BOLD           = 0001
ITALIC         = 0010
UNDERLINE      = 0100
STRIKETHROUGH  = 1000
```
</div>

[بازگشت به بالا](#top)

---

<a id="combine"></a>
## ۳. چگونه چند Style را با هم ترکیب کنیم؟

با OR:

<div dir="ltr">

```java
text.applyStyles(STYLE_BOLD | STYLE_ITALIC);
```
</div>

محاسبه:

<div dir="ltr">

```text
  0001   BOLD
| 0010   ITALIC
------
  0011
```
</div>

پس `0011` یعنی `BOLD + ITALIC`.

[بازگشت به بالا](#top)

---

<a id="performance-appeal"></a>
## ۴. چرا Bit Field از نظر Performance جذاب بود؟

چون یک عدد می‌تواند نماینده‌ی مجموعه‌ای از flagها باشد:

<div dir="ltr">

```text
int
│
├── bit 0 → BOLD
├── bit 1 → ITALIC
├── bit 2 → UNDERLINE
├── bit 3 → STRIKETHROUGH
├── ...
└── bit 31
```
</div>

بنابراین `STYLE_BOLD | STYLE_ITALIC` خیلی سریع انجام می‌شود.

Intersection: `styles & STYLE_BOLD`

Union: `styles1 | styles2`

حذف: `styles & ~STYLE_BOLD`

با عملیات بسیار ارزان bitwise. بنابراین در نگاه اول طراحی بسیار جذابی است.

[بازگشت به بالا](#top)

---

<a id="the-problem"></a>
## ۵. پس مشکل چیست؟

مشکل این است که ما **Performance را به قیمت از دست دادن abstraction و Type Safety** به دست آورده‌ایم.

این API:

<div dir="ltr">

```java
public void applyStyles(int styles)
```
</div>

از نظر type system تقریباً هیچ اطلاعاتی نمی‌دهد.

هر `int`ای می‌تواند وارد آن شود:

<div dir="ltr">

```java
text.applyStyles(123456);   // ✅ Compile
text.applyStyles(42);       // ✅ Compile
text.applyStyles(-1);       // ✅ Compile
text.applyStyles(Integer.MAX_VALUE); // ✅ Compile
```
</div>

Compiler اصلاً نمی‌داند: «این عدد باید مجموعه‌ای از Styleها باشد.»

[بازگشت به بالا](#top)

---

<a id="worse-than-item34"></a>
## ۶. مشکل حتی بدتر از Item 34 است

در Item 34 مشکل داشتیم:

<div dir="ltr">

```java
public static final int BOLD = 1;
public static final int ITALIC = 2;
```
</div>

اما اینجا یک مشکل اضافه داریم. ما دیگر فقط یک مقدار `int` نداریم. یک `int` در واقع **چند مفهوم مختلف را به صورت فشرده داخل خودش نگه داشته است.**

مثلاً `3` یعنی:

<div dir="ltr">

```text
0011  →  BOLD + ITALIC
```
</div>

ولی اگر این را log کنیم:

<div dir="ltr">

```java
log.info("styles={}", styles);
```
</div>

ممکن است ببینیم:

<div dir="ltr">

```text
styles=3
```
</div>

یک نفر که کد را می‌بیند باید بداند: `3 = 0011 = BOLD + ITALIC`. این بسیار بدتر از `[BOLD, ITALIC]` است.

[بازگشت به بالا](#top)

---

<a id="iteration-problem"></a>
## ۷. مشکل دوم: Iteration

فرض کنید می‌خواهیم تمام Styleهای فعال را iterate کنیم. با Bit Field باید چیزی شبیه این بنویسیم:

<div dir="ltr">

```java
if ((styles & STYLE_BOLD) != 0) { /* BOLD */ }
if ((styles & STYLE_ITALIC) != 0) { /* ITALIC */ }
if ((styles & STYLE_UNDERLINE) != 0) { /* UNDERLINE */ }
if ((styles & STYLE_STRIKETHROUGH) != 0) { /* STRIKETHROUGH */ }
```
</div>

با اضافه شدن Style جدید باید این منطق را هم بررسی کنیم.

[بازگشت به بالا](#top)

---

<a id="capacity-limit"></a>
## ۸. مشکل سوم: ظرفیت محدود

اگر از `int` استفاده کنیم: 32 bits → حداکثر 32 flag.

اگر از `long` استفاده کنیم: 64 bits.

فرض کنید API شما `public void applyStyles(long styles)` باشد. بعداً به 65 Style نیاز پیدا کنید. دیگر نمی‌توانیم همان representation را حفظ کنیم.

این یعنی یک محدودیت implementation وارد API عمومی شده است.

[بازگشت به بالا](#top)

---

<a id="abstraction"></a>
## ۹. این دقیقاً چیزی است که Effective Java می‌خواهد از آن جلوگیری کند

ما نباید API را این‌گونه طراحی کنیم:

<div dir="ltr">

```text
Domain Model → Bit-level representation → Public API
```
</div>

بلکه:

<div dir="ltr">

```text
Domain Model → Set<Style> → Implementation → EnumSet / optimized representation
```
</div>

باید abstraction را از implementation جدا کنیم.

[بازگشت به بالا](#top)

---

<a id="modern-solution"></a>
## ۱۰. راه‌حل مدرن: Enum + EnumSet

اول یک Enum:

<div dir="ltr">

```java
public enum Style {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH
}
```
</div>

حالا به جای `int styles` می‌گوییم `Set<Style> styles`:

<div dir="ltr">

```java
Set<Style> styles = EnumSet.of(Style.BOLD, Style.ITALIC);
```
</div>

Representation از نظر مفهومی کاملاً واضح است: `[BOLD, ITALIC]`

[بازگشت به بالا](#top)

---

<a id="api-design"></a>
## ۱۱. API را چگونه طراحی کنیم؟

کتاب پیشنهاد می‌کند:

<div dir="ltr">

```java
public class Text {

    public enum Style {
        BOLD,
        ITALIC,
        UNDERLINE,
        STRIKETHROUGH
    }

    public void applyStyles(Set<Style> styles) {
        // ...
    }
}
```
</div>

و client:

<div dir="ltr">

```java
text.applyStyles(
    EnumSet.of(
        Text.Style.BOLD,
        Text.Style.ITALIC
    )
);
```
</div>

این API از نظر Type System بسیار بهتر است.

[بازگشت به بالا](#top)

---

<a id="type-safety-change"></a>
## ۱۲. Type Safety چه تغییری کرده؟

در Bit Field:

<div dir="ltr">

```java
public void applyStyles(int styles)
```
</div>

این کاملاً قانونی بود:

<div dir="ltr">

```java
text.applyStyles(123);  // ✅ Compile
```
</div>

اما در API جدید:

<div dir="ltr">

```java
public void applyStyles(Set<Style> styles)
```
</div>

این:

<div dir="ltr">

```java
text.applyStyles(123);  // ❌ Compile Error
```
</div>

اصلاً compile نمی‌شود.

حتی:

<div dir="ltr">

```java
Set<Color> colors = ...;
text.applyStyles(colors);  // ❌ Compile Error
```
</div>

چون `Set<Color>` با `Set<Style>` متفاوت است. این دقیقاً همان **Type Safety** است.

[بازگشت به بالا](#top)

---

<a id="what-is-enumset"></a>
## ۱۳. EnumSet دقیقاً چیست؟

`EnumSet` یک implementation تخصصی از `Set<E>` برای Enumهاست.

<div dir="ltr">

```java
EnumSet<Style>
```
</div>

در واقع یک Set است که می‌داند عناصر آن از یک enum خاص می‌آیند. فقط می‌تواند `Style.BOLD`، `Style.ITALIC`، `Style.UNDERLINE` و `Style.STRIKETHROUGH` داشته باشد.

[بازگشت به بالا](#top)

---

<a id="internal-bitfield"></a>
## ۱۴. قسمت بسیار جالب: EnumSet در داخل تقریباً همان Bit Field است!

ممکن است بگوییم: "خب، Bit Field سریع است؛ پس چرا `EnumSet` استفاده کنیم؟"

چون `EnumSet` در implementation داخلی خودش از representation بسیار فشرده و bit-based استفاده می‌کند. برای Enumهایی که حداکثر 64 عضو دارند، می‌تواند مجموعه را بسیار فشرده با یک `long` نمایش دهد.

<div dir="ltr">

```text
EnumSet<Style>
       │
       ▼
Bit representation
       │
       ▼
very compact
```
</div>

اما شما مجبور نیستید خودتان `<<`، `|`، `&` و `~` را مدیریت کنید.

[بازگشت به بالا](#top)

---

<a id="enumset-philosophy"></a>
## ۱۵. یعنی `EnumSet` در واقع این فلسفه را دارد

<div dir="ltr">

```text
Bit Field
+
Type Safety
+
Set API
+
Readable Code
+
Iteration
+
No manual bit manipulation
+
No 32/64-bit API limitation
```
</div>

این دقیقاً دلیل اصلی توصیه‌ی Item 36 است.

[بازگشت به بالا](#top)

---

<a id="direct-comparison"></a>
## ۱۶. مقایسه مستقیم

| ویژگی | Bit Field | `EnumSet` |
|--------|-----------|-----------|
| Type Safety | ❌ | ✅ |
| Readability | ❌ | ✅ |
| Iteration | سخت | بسیار ساده |
| Set API | ❌ | ✅ |
| Union | `\|` | `addAll` |
| Intersection | `&` | `retainAll` |
| Remove | `& ~` | `remove` |
| Logging | ضعیف | بهتر |
| Debugging | ضعیف | بهتر |
| ظرفیت fixed به 32/64 | ✅ | ❌ |
| وابستگی به bit manipulation | زیاد | ❌ |
| Performance | عالی | بسیار خوب |
| API abstraction | ضعیف | عالی |

[بازگشت به بالا](#top)

---

<a id="set-operations"></a>
## ۱۷. عملیات Set چگونه تبدیل می‌شوند؟

### Union

Bit Field:

<div dir="ltr">

```java
styles1 | styles2
```
</div>

EnumSet:

<div dir="ltr">

```java
EnumSet<Style> result = EnumSet.copyOf(styles1);
result.addAll(styles2);
```
</div>

### Intersection

Bit Field:

<div dir="ltr">

```java
styles1 & styles2
```
</div>

EnumSet:

<div dir="ltr">

```java
EnumSet<Style> result = EnumSet.copyOf(styles1);
result.retainAll(styles2);
```
</div>

### Difference

Bit Field:

<div dir="ltr">

```java
styles1 & ~styles2
```
</div>

EnumSet:

<div dir="ltr">

```java
EnumSet<Style> result = EnumSet.copyOf(styles1);
result.removeAll(styles2);
```
</div>

می‌بینی که:

<div dir="ltr">

```text
Bitwise language → Domain language
```
</div>

و این از نظر طراحی نرم‌افزار بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="set-vs-enumset"></a>
## ۱۸. چرا `Set<Style>` و نه `EnumSet<Style>`؟

ممکن است وسوسه شویم:

<div dir="ltr">

```java
public void applyStyles(EnumSet<Style> styles)
```
</div>

اما کتاب پیشنهاد می‌کند:

<div dir="ltr">

```java
public void applyStyles(Set<Style> styles)
```
</div>

چرا؟ چون باید تا حد امکان **interface type** را در API بپذیریم، نه implementation type.

<div dir="ltr">

```text
❌ EnumSet<Style>   ← implementation detail
✅ Set<Style>       ← abstraction
```
</div>

[بازگشت به بالا](#top)

---

<a id="api-design-principle"></a>
## ۱۹. اصل مهم API Design

فرض کنید امروز `EnumSet<Style>` استفاده می‌کنیم. اما یک client خاص ممکن است بخواهد:

<div dir="ltr">

```java
Set<Style> styles = Collections.unmodifiableSet(...);
Set<Style> styles = someCustomSet;
```
</div>

اگر API بگوید `void applyStyles(EnumSet<Style> styles)`، آن client نمی‌تواند Set خودش را پاس بدهد.

ولی `void applyStyles(Set<Style> styles)` انعطاف بسیار بیشتری دارد.

> **Program to interfaces, not implementations.**

[بازگشت به بالا](#top)

---

<a id="enumset-passing"></a>
## ۲۰. اما اگر همه EnumSet بدهند چه؟

هیچ مشکلی ندارد.

<div dir="ltr">

```java
EnumSet<Style> styles =
        EnumSet.of(Style.BOLD, Style.ITALIC);
```
</div>

به این متد کاملاً قابل قبول است:

<div dir="ltr">

```java
void applyStyles(Set<Style> styles)
```
</div>

چون `EnumSet<Style>` → `implements` → `Set<Style>`.

[بازگشت به بالا](#top)

---

<a id="factories"></a>
## ۲۱. Factoryهای بسیار خوب EnumSet

### چند عضو مشخص:

<div dir="ltr">

```java
EnumSet.of(Style.BOLD, Style.ITALIC);
```
</div>

### مجموعه خالی:

<div dir="ltr">

```java
EnumSet.noneOf(Style.class);
```
</div>

### همه اعضا:

<div dir="ltr">

```java
EnumSet.allOf(Style.class);
```
</div>

### Complement

<div dir="ltr">

```java
EnumSet.complementOf(EnumSet.of(Style.BOLD));
```
</div>

### Range

<div dir="ltr">

```java
EnumSet.range(Day.MONDAY, Day.FRIDAY);
```
</div>

می‌دهد: `MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY`

این API بسیار expressive‌تر از Bit Field است.

[بازگشت به بالا](#top)

---

<a id="iteration-better"></a>
## ۲۲. Iteration چقدر بهتر می‌شود؟

با Bit Field:

<div dir="ltr">

```java
if ((styles & STYLE_BOLD) != 0) ...
if ((styles & STYLE_ITALIC) != 0) ...
if ((styles & STYLE_UNDERLINE) != 0) ...
```
</div>

اما با EnumSet:

<div dir="ltr">

```java
for (Style style : styles) {
    apply(style);
}
```
</div>

یا:

<div dir="ltr">

```java
styles.forEach(this::apply);
```
</div>

این کد دقیقاً چیزی را بیان می‌کند که Domain می‌خواهد: برای هر Style فعال، عملیات را انجام بده.

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## ۲۳. یک Production Example

<div dir="ltr">

```java
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    IN_APP
}
```
</div>

API:

<div dir="ltr">

```java
public void send(
        Notification notification,
        Set<NotificationChannel> channels
) {
    for (NotificationChannel channel : channels) {
        sendToChannel(notification, channel);
    }
}
```
</div>

Client:

<div dir="ltr">

```java
Set<NotificationChannel> channels =
        EnumSet.of(
            NotificationChannel.EMAIL,
            NotificationChannel.PUSH
        );

service.send(notification, channels);
```
</div>

این بسیار بهتر از `service.send(notification, EMAIL | PUSH);` است.

[بازگشت به بالا](#top)

---

<a id="anti-pattern-example"></a>
## ۲۴. Anti-Pattern همین مثال

<div dir="ltr">

```java
public static final int EMAIL  = 1 << 0;
public static final int SMS    = 1 << 1;
public static final int PUSH   = 1 << 2;
public static final int IN_APP = 1 << 3;

public void send(Notification notification, int channels) { }
```
</div>

Client:

<div dir="ltr">

```java
service.send(notification, EMAIL | PUSH);
```
</div>

مشکلات:

<div dir="ltr">

```text
❌ int type
❌ magic representation
❌ bitwise operations
❌ difficult debugging
❌ weak API contract
❌ difficult iteration
❌ manual validation
```
</div>

[بازگشت به بالا](#top)

---

<a id="enumset-null"></a>
## ۲۵. نکته مهم درباره `EnumSet` و `null`

`EnumSet` نمی‌تواند `null` را به عنوان عنصر نگه دارد:

<div dir="ltr">

```java
EnumSet<Style> styles = EnumSet.noneOf(Style.class);
styles.add(null);  // ❌
```
</div>

این از نظر Domain معمولاً نکته مثبتی است، چون `null` اصلاً یک Style معتبر نیست.

[بازگشت به بالا](#top)

---

<a id="enumset-specific"></a>
## ۲۶. EnumSet برای یک Enum خاص است

<div dir="ltr">

```java
EnumSet<Style>
```
</div>

فقط Style است. نمی‌توانیم `styles.add(Color.RED)` انجام دهیم. این دقیقاً همان Type Safety است که Bit Field فاقد آن بود.

در Bit Field:

<div dir="ltr">

```java
int styles
```
</div>

هیچ distinction واقعی بین این‌ها وجود ندارد:

<div dir="ltr">

```text
Style flags
Permission flags
Feature flags
Access flags
```
</div>

همه `int` هستند.

[بازگشت به بالا](#top)

---

<a id="dangerous-api"></a>
## ۲۷. Bit Field حتی می‌تواند APIهای خطرناک ایجاد کند

<div dir="ltr">

```java
public void updateUser(int flags)
```
</div>

و دو نوع flag داریم:

<div dir="ltr">

```java
USER_ACTIVE
USER_ADMIN

NOTIFICATION_EMAIL
NOTIFICATION_SMS
```
</div>

همه `int` هستند. حالا امکان اشتباه وجود دارد:

<div dir="ltr">

```java
updateUser(NOTIFICATION_EMAIL);  // ✅ Compile، ❌ Semantics
```
</div>

Compiler هیچ چیزی نمی‌گوید. اما با Enum:

<div dir="ltr">

```java
Set<UserFlag>   // نمی‌توانیم Set<NotificationFlag> را جای آن بدهیم
```
</div>

[بازگشت به بالا](#top)

---

<a id="performance-note"></a>
## ۲۸. یک نکته مهم Performance

ممکن است کسی بگوید: "ولی `HashSet` هم Set است؛ چرا از HashSet استفاده نکنیم؟"

چون اگر عناصر Enum باشند، `EnumSet` دقیقاً برای این سناریو optimize شده است. `EnumSet<Style>` معمولاً از `HashSet<Style>` کم‌هزینه‌تر است، چون EnumSet از ساختار داخلی بسیار specialized استفاده می‌کند.

بنابراین `Enum values → Set semantics → EnumSet` انتخاب طبیعی است.

[بازگشت به بالا](#top)

---

<a id="enumset-vs-hashset"></a>
## ۲۹. EnumSet در برابر HashSet

| ویژگی | `EnumSet` | `HashSet` |
|--------|-----------|-----------|
| فقط Enum | ✅ | ❌ |
| Specialized | ✅ | ❌ |
| Type-safe | ✅ | ✅ |
| حافظه | بسیار efficient | بیشتر |
| عملیات Set | بسیار سریع | سریع |
| null | ❌ | معمولاً ممکن |
| مناسب Enum | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| عمومی برای هر Object | ❌ | ✅ |

بنابراین اگر `Set<MyEnum>` داریم، اولین گزینه‌ای که باید بررسی کنیم `EnumSet<MyEnum>` است.

[بازگشت به بالا](#top)

---

<a id="capacity-note"></a>
## ۳۰. یک نکته ظریف درباره ظرفیت EnumSet

کتاب می‌گوید برای Enumهای دارای 64 عضو یا کمتر، representation می‌تواند با یک `long` انجام شود:

<div dir="ltr">

```text
Enum
 ├── A → bit 0
 ├── B → bit 1
 ├── C → bit 2
 ...
 └── 64th → bit 63
```
</div>

اگر بیشتر از 64 عضو باشد، implementation می‌تواند از چند word استفاده کند.

بنابراین برخلاف Bit Field عمومی (`int → 32`, `long → 64`)، API شما مجبور نیست بگوید: "حداکثر 64 enum constant داریم." این جزئیات را `EnumSet` مدیریت می‌کند.

[بازگشت به بالا](#top)

---

<a id="representation-independence"></a>
## ۳۱. یک نکته معماری بسیار مهم: Representation Independence

این Item در سطح عمیق‌تر درباره‌ی **Representation Independence** است.

Client باید بداند `Set<Style>`، نه اینکه بداند یک `long` که bitهایش set شده‌اند.

<div dir="ltr">

```text
Client
  │
  ▼
Set<Style>       ← Contract
  │
  ▼
EnumSet          ← Implementation
  │
  ▼
Bit Vector       ← Internal Representation
```
</div>

این separation فوق‌العاده ارزشمند است. اگر implementation داخلی فردا تغییر کند، client نباید تحت تأثیر قرار بگیرد.

[بازگشت به بالا](#top)

---

<a id="immutable-enumset"></a>
## ۳۲. Immutable EnumSet چه؟

در طراحی فعلی Java، `EnumSet` خودش mutable است:

<div dir="ltr">

```java
styles.add(...)
styles.remove(...)
```
</div>

اگر API شما باید مجموعه‌ای immutable داشته باشد، می‌توانید آن را wrap کنید:

<div dir="ltr">

```java
Set<Style> styles =
        Collections.unmodifiableSet(
            EnumSet.of(Style.BOLD, Style.ITALIC)
        );
```
</div>

نکته‌ی اصلی:

<div dir="ltr">

```text
EnumSet = specialized mutable Set
```
</div>

نه: `EnumSet = immutable Set`

[بازگشت به بالا](#top)

---

<a id="set-of"></a>
## ۳۳. آیا `Set.of()` جایگزین EnumSet است؟

<div dir="ltr">

```java
Set<Style> styles = Set.of(Style.BOLD, Style.ITALIC);
```
</div>

این هم کاملاً type-safe و immutable است. اما اگر شما مشخصاً به semantics و performance تخصصی Enumها نیاز دارید، `EnumSet` انتخاب طبیعی‌تری است:

<div dir="ltr">

```java
EnumSet<Style> styles = EnumSet.noneOf(Style.class);
styles.add(Style.BOLD);
styles.add(Style.ITALIC);
styles.remove(Style.BOLD);
```
</div>

این دقیقاً سناریوی مناسب EnumSet است.

[بازگشت به بالا](#top)

---

<a id="chain-34-35-36"></a>
## ۳۴. ارتباط Item 34 → 35 → 36

### Item 34

<div dir="ltr">

```text
int constants → Enum
```
</div>

### Item 35

<div dir="ltr">

```text
ordinal() → explicit instance field
```
</div>

### Item 36

<div dir="ltr">

```text
int bit fields → EnumSet
```
</div>

یعنی Effective Java قدم‌به‌قدم ما را از این:

<div dir="ltr">

```text
Old Java
──────────────
int
int ordinal
int bit field
```
</div>

به این می‌برد:

<div dir="ltr">

```text
Modern Java
──────────────
Enum
explicit fields
EnumSet
```
</div>

[بازگشت به بالا](#top)

---

<a id="type-system-chain"></a>
## ۳۵. و این زنجیره از نظر Type System خیلی مهم است

مدل قدیمی:

<div dir="ltr">

```java
int flags
```
</div>

در واقع می‌گوید: "من به Compiler اعتماد ندارم؛ خودم با bitها قرارداد را مدیریت می‌کنم."

مدل جدید:

<div dir="ltr">

```java
Set<Style>
```
</div>

می‌گوید: "Compiler و Type System باید بخشی از قرارداد API را enforce کنند."

این همان فلسفه‌ای است که در Items 26 تا 33 هم دیدیم:

<div dir="ltr">

```text
Raw Types → Unchecked Warnings → Arrays vs Generics
    → Bounded Wildcards → Varargs + Generics
    → Typesafe Heterogeneous Containers → Enums → EnumSet
```
</div>

در همه‌ی این‌ها یک Theme مشترک وجود دارد:

> **تا جای ممکن خطا را از Runtime به Compile Time منتقل کن.**

[بازگشت به بالا](#top)

---

<a id="anti-vs-best"></a>
## ۳۶. Anti-Pattern vs Best Practice

### ❌ Anti-Pattern

<div dir="ltr">

```java
public static final int BOLD = 1;
public static final int ITALIC = 2;
public static final int UNDERLINE = 4;

public void applyStyles(int styles) {
    if ((styles & BOLD) != 0) { }
}
```
</div>

Client:

<div dir="ltr">

```java
text.applyStyles(BOLD | ITALIC);
```
</div>

### ✅ Best Practice

<div dir="ltr">

```java
public enum Style { BOLD, ITALIC, UNDERLINE }

public void applyStyles(Set<Style> styles) {
    if (styles.contains(Style.BOLD)) { }
}
```
</div>

Client:

<div dir="ltr">

```java
text.applyStyles(EnumSet.of(Style.BOLD, Style.ITALIC));
```
</div>

[بازگشت به بالا](#top)

---

<a id="production-grade"></a>
## ۳۷. Production-Grade Version

<div dir="ltr">

```java
public final class TextFormatter {

    public enum Style {
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH
    }

    public void applyStyles(Set<Style> styles) {
        Objects.requireNonNull(styles, "styles");

        for (Style style : styles) {
            apply(style);
        }
    }

    private void apply(Style style) {
        switch (style) {
            case BOLD -> applyBold();
            case ITALIC -> applyItalic();
            case UNDERLINE -> applyUnderline();
            case STRIKETHROUGH -> applyStrikethrough();
        }
    }
}
```
</div>

Client:

<div dir="ltr">

```java
formatter.applyStyles(
    EnumSet.of(
        TextFormatter.Style.BOLD,
        TextFormatter.Style.ITALIC
    )
);
```
</div>

در اینجا API می‌گوید: "I accept a set of Styles."

نه: "Give me an integer whose bits have a secret meaning."

این تفاوت، تفاوت بین **API طراحی‌شده برای انسان و API طراحی‌شده برای ماشین** است.

[بازگشت به بالا](#top)

---

<a id="when-bitfield-ok"></a>
## ۳۸. چه زمانی Bit Field هنوز قابل قبول است؟

اینجا باید کمی nuance داشته باشیم.

Bitmask به‌عنوان یک تکنیک پایین‌سطح هنوز در بعضی حوزه‌ها وجود دارد:

<div dir="ltr">

```text
Operating systems
Network protocols
Hardware registers
Binary file formats
JNI/native APIs
Memory-constrained systems
Low-level serialization
```
</div>

مثلاً یک protocol ممکن است واقعاً یک `uint32` داشته باشد که هر bit یک flag باشد. در آنجا Bitmask بخشی از **external binary contract** است.

ولی اگر خودت در Java یک Domain Model طراحی می‌کنی:

<div dir="ltr">

```java
Set<Permission>
```
</div>

به‌مراتب بهتر از `int permissionFlags` است.

[بازگشت به بالا](#top)

---

<a id="decision-framework"></a>
## ۳۹. Decision Framework

### سؤال اول

آیا مجموعه‌ای از enumها داریم؟

<div dir="ltr">

```text
YES → EnumSet
```
</div>

### سؤال دوم

آیا API عمومی است؟

<div dir="ltr">

```text
YES → Set<MyEnum>
```
</div>

نه `EnumSet<MyEnum>`، مگر اینکه واقعاً implementation-specific constraint داشته باشی.

### سؤال سوم

آیا external protocol دقیقاً bitmask تعریف کرده؟

<div dir="ltr">

```text
YES → Bitmask ممکن است مناسب باشد
```
</div>

اما بهتر است boundary را از Domain Model جدا کنی:

<div dir="ltr">

```text
External Bitmask → Adapter / Mapper → EnumSet<Permission> → Domain
```
</div>

این معماری بسیار تمیزتر است.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۴۰. خلاصه نهایی Item 36

اصل Item را می‌توان در یک جمله خلاصه کرد:

> **اگر یک Enum را به شکل Set استفاده می‌کنی، `EnumSet` تقریباً همیشه انتخاب بهتری از Bit Field است.**

چرا؟

<div dir="ltr">

```text
EnumSet
  │
  ├── Type Safety             ✅
  ├── Readability             ✅
  ├── Set API                 ✅
  ├── Iteration               ✅
  ├── Extensibility           ✅
  ├── Interface-based API     ✅
  ├── Compact representation  ✅
  └── Bitwise performance     ✅
```
</div>

در مقابل:

<div dir="ltr">

```text
Bit Field
  │
  ├── int/long only           ❌
  ├── weak type safety        ❌
  ├── unreadable values       ❌
  ├── manual bit logic        ❌
  ├── difficult iteration     ❌
  └── fixed bit capacity      ❌
```
</div>

### مهم‌ترین جمله‌ای که از Item 36 باید در ذهن بماند:

<div dir="ltr">

```text
Don't expose the bits.
Expose the Set.
```
</div>

یعنی اگر زیرساخت داخلی واقعاً می‌خواهد از bitwise arithmetic استفاده کند، **بگذار خود `EnumSet` این کار را انجام دهد**؛ API و Domain Model تو باید با مفاهیم سطح بالاتر یعنی `Enum` و `Set<Enum>` کار کنند.

و این دقیقاً یک مثال عالی از یک اصل معماری است:

> **Implementation را optimize کن، ولی abstraction را قربانی optimization نکن.**

---

[بازگشت به بالا](#top)

</div>
