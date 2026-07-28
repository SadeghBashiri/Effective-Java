<div dir="rtl">

<a id="top"></a>

# آیتم ۱۲: همیشه toString را Override کنید (Always override toString)

به **Item 12** رسیدیم. این آیتم در نگاه اول ساده به نظر می‌رسد، اما در پروژه‌های Enterprise یکی از پرکاربردترین متدهای هر کلاس است. تقریباً هر Log، Debugger، Exception، تست، مانیتورینگ و حتی بسیاری از Frameworkها به نحوی از `toString()` استفاده می‌کنند.

نکته جالب این است که **Joshua Bloch این آیتم را بعد از `equals()` و `hashCode()` آورده است**؛ یعنی پس از اینکه هویت منطقی شیء را تعریف کردیم، اکنون باید نحوه نمایش آن را برای انسان تعریف کنیم.

---

## فهرست مطالب

- [هدف اصلی Item 12](#core-goal)
- [پیاده‌سازی پیش‌فرض Object](#default-implementation)
- [چیزی که انسان می‌خواهد](#what-humans-want)
- [قرارداد toString](#contract)
- [چرا اینقدر مهم است؟](#why-important)
- [Debugging](#debugging)
- [Collection‌ها](#collections)
- [چه چیزهایی باید داخل toString باشند؟](#what-to-include)
- [اگر شیء خیلی بزرگ باشد؟](#large-objects)
- [تصمیم مهم: Format رسمی یا غیررسمی؟](#format-decision)
  - [حالت اول: Format رسمی](#formal-format)
  - [حالت دوم: Format غیررسمی](#informal-format)
- [نکته مهم: همیشه Getter داشته باشید](#always-have-getters)
- [چه کلاس‌هایی نباید toString بنویسند؟](#when-not-to-override)
- [AutoValue و IDE](#autovalue-and-ide)
- [Production-Grade Example](#production-example)
- [Anti-Patternهای رایج](#anti-patterns)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-goal"></a>
## هدف اصلی Item 12

اگر یک کلاس قابل Instantiate می‌نویسی، تقریباً همیشه باید `toString()` را Override کنی.

چرا؟

زیرا پیاده‌سازی پیش‌فرض Object تقریباً هیچ اطلاعات مفیدی ارائه نمی‌دهد.

[بازگشت به بالا](#top)

---

<a id="default-implementation"></a>
## پیاده‌سازی پیش‌فرض Object

فرض کنید:

<div dir="ltr">

```java
public class PhoneNumber {
    private final int areaCode;
    private final int prefix;
    private final int lineNumber;
}
```
</div>

اگر بنویسیم:

<div dir="ltr">

```java
PhoneNumber phone = new PhoneNumber(707, 867, 5309);
System.out.println(phone);
```
</div>

خروجی:

```text
PhoneNumber@163b91
```

یا

```text
com.example.PhoneNumber@6d06d69c
```

این رشته از دو قسمت تشکیل شده است:

```
ClassName + @ + Hex HashCode
```

مثلاً `PhoneNumber@163b91` یعنی `PhoneNumber` + `HashCode = 0x163b91`.

از دید JVM این کافی است. اما از دید برنامه‌نویس؟ **تقریباً بی‌فایده است.**

[بازگشت به بالا](#top)

---

<a id="what-humans-want"></a>
## چیزی که انسان می‌خواهد

برای `PhoneNumber` بهتر است:

```
707-867-5309
```

یا

```
(+1) 707-867-5309
```

یعنی نمایش باید:

- کوتاه باشد
- خوانا باشد
- معنی‌دار باشد

[بازگشت به بالا](#top)

---

<a id="contract"></a>
## قرارداد toString

کتاب می‌گوید:

> concise but informative representation

یعنی **نه خیلی طولانی، نه خیلی مبهم**، بلکه **مختصر ولی مفید**.

[بازگشت به بالا](#top)

---

<a id="why-important"></a>
## چرا اینقدر مهم است؟

فرض کنید:

<div dir="ltr">

```java
PhoneNumber phone = ...
```
</div>

بعد:

<div dir="ltr">

```java
System.out.println(phone);
```
</div>

یا:

<div dir="ltr">

```java
logger.info("Phone = {}", phone);
```
</div>

یا:

<div dir="ltr">

```java
assertEquals(expected, actual);
```
</div>

یا:

<div dir="ltr">

```java
throw new RuntimeException(phone.toString());
```
</div>

در تمام این موارد `toString()` به صورت خودکار صدا زده می‌شود.

[بازگشت به بالا](#top)

---

<a id="debugging"></a>
## Debugging

فرض کنید Log داریم:

```
Failed to connect to PhoneNumber@7828ab
```

آیا چیزی می‌فهمیم؟ خیر.

اگر `toString` درست نوشته شود:

```
Failed to connect to 707-867-5309
```

اکنون Log کاملاً قابل فهم است.

[بازگشت به بالا](#top)

---

<a id="collections"></a>
## Collection‌ها

کتاب مثال بسیار خوبی می‌زند.

فرض کنید `Map<String, PhoneNumber>` داریم.

اگر `toString` نداشته باشیم:

```
{Jenny=PhoneNumber@163b91}
```

اما اگر داشته باشیم:

```
{Jenny=707-867-5309}
```

به همین دلیل است که کلاس‌های استاندارد جاوا تقریباً همگی `toString()` مناسبی دارند.

[بازگشت به بالا](#top)

---

<a id="what-to-include"></a>
## چه چیزهایی باید داخل toString باشند؟

کتاب می‌گوید: **تمام اطلاعات مهم شیء، تا جایی که منطقی باشد.**

مثلاً `Customer` می‌تواند شامل:

- `id`
- `name`
- `status`

باشد.

اما موارد زیر را **نباید** نمایش داد:

- `Password`
- `SecretKey`
- `JWT`
- `CreditCard CVV`

[بازگشت به بالا](#top)

---

<a id="large-objects"></a>
## اگر شیء خیلی بزرگ باشد؟

مثلاً:

- Database با ۱۰ میلیون رکورد
- Cache با ۵۰۰ هزار آیتم

نمایش همه اطلاعات اشتباه است.

به جای آن، خلاصه‌ای از وضعیت:

```text
CustomerCache[size=542000, hits=90%, misses=10%]
```

### مثال Thread

کتاب مثال می‌زند:

```text
Thread[main,5,main]
```

اطلاعات کامل نیست ولی خلاصه مناسبی است.

[بازگشت به بالا](#top)

---

<a id="format-decision"></a>
## تصمیم مهم: Format رسمی یا غیررسمی؟

Joshua Bloch می‌گوید قبل از نوشتن `toString`، یک سؤال از خودت بپرس:

> آیا Format رسمی است یا فقط برای Debug؟

این **مهم‌ترین بخش Item** است.

<a id="formal-format"></a>
### حالت اول: Format رسمی

مثلاً `PhoneNumber`:

```
707-867-5309
```

این یک استاندارد است. اگر آن را مستند کنیم، بعدها دیگر حق تغییر Format را نداریم. چون کاربران ممکن است آن را Parse کنند:

- CSV
- Excel
- Database
- JSON
- Config

همگی ممکن است همین رشته را ذخیره کنند.

در این حالت کتاب پیشنهاد می‌کند مستند بنویسیم:

<div dir="ltr">

```java
/**
 * Returns a string representation of this phone number in the format:
 * XXX-YYY-ZZZZ
 */
```
</div>

همچنین یک Factory Method یا Constructor متناظر ارائه کنیم:

<div dir="ltr">

```java
PhoneNumber.from("707-867-5309");
```
</div>

این دقیقاً کاری است که `BigInteger`، `BigDecimal`، `Integer`، `UUID` و ... انجام داده‌اند.

<a id="informal-format"></a>
### حالت دوم: Format غیررسمی

مثلاً:

- `Potion`
- `Monster`
- `EmployeeContext`
- `RequestContext`

در این حالت می‌توان نوشت:

<div dir="ltr">

```java
/**
 * Returns a string representation of this object.
 * The format is unspecified and may change.
 */
```
</div>

یعنی: فقط برای نمایش است. حق Parse کردن آن را نداری.

[بازگشت به بالا](#top)

---

<a id="always-have-getters"></a>
## نکته مهم: همیشه Getter داشته باشید

حتی اگر Format را مستند نکرده‌ای، باز هم باید Getter داشته باشی.

مثلاً `PhoneNumber` نباید فقط `toString()` داشته باشد. بلکه:

<div dir="ltr">

```java
getAreaCode()
getPrefix()
getLineNumber()
```
</div>

نیز لازم هستند.

چرا؟ اگر Getter وجود نداشته باشد، کاربر مجبور می‌شود String را Parse کند:

<div dir="ltr">

```java
String[] parts = phone.toString().split("-");
```
</div>

این یکی از بدترین Anti-Patternها است، زیرا با کوچک‌ترین تغییر در قالب رشته، کد کاربران می‌شکند و در عمل `toString()` به یک API رسمی تبدیل می‌شود.

[بازگشت به بالا](#top)

---

<a id="when-not-to-override"></a>
## چه کلاس‌هایی نباید toString بنویسند؟

کتاب می‌گوید Utility Class مثل `MathUtils` یا `FileUtils` اصلاً Instance ندارند. پس `toString` بی‌معنی است.

همچنین بیشتر `enum`ها نیازی ندارند، چون جاوا از قبل یک `toString` مناسب ارائه کرده است.

اما Abstract Class اگر نمایش مشترکی بین Subclassها وجود داشته باشد، بهتر است `toString` را همانجا پیاده‌سازی کند.

[بازگشت به بالا](#top)

---

<a id="autovalue-and-ide"></a>
## AutoValue و IDE

IDEها می‌توانند به صورت خودکار این را تولید کنند:

```text
User{id=1, name='Ali', age=30}
```

برای بسیاری از کلاس‌ها این کافی است، اما برای کلاس‌هایی که نمایش استاندارد دامنه (Domain) دارند، مانند `PhoneNumber` یا `Money`، بهتر است `toString()` اختصاصی نوشته شود.

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## Production-Grade Example

فرض کنید کلاس `Customer`:

<div dir="ltr">

```java
public final class Customer {

    private final UUID id;
    private final String fullName;
    private final String email;
    private final CustomerStatus status;

    public Customer(UUID id, String fullName, String email, CustomerStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public CustomerStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "Customer[" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ']';
    }
}
```
</div>

خروجی:

```text
Customer[id=4f34a8b9-8c1d-4e2f-9a3b-7c6d5e4f3a2b, fullName='John Smith', email='john@example.com', status=ACTIVE]
```

این خروجی برای Log و Debug مناسب است.

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternهای رایج

| Anti-Pattern | مشکل |
|--------------|------|
| **عدم Override** | خروجی `ClassName@HashCode` بی‌فایده است |
| **نمایش اطلاعات محرمانه** | `Password`، `Token`، `CVV` در Log ثبت می‌شود |
| **وابسته کردن API به `toString()`** | `toString()` را Parse کردن باعث وابستگی به قالب می‌شود |

### مثال Anti-Pattern شماره ۲

<div dir="ltr">

```java
@Override
public String toString() {
    return "User{" +
            "username=" + username +
            ", password=" + password +   // ❌
            ", token=" + jwt +           // ❌
            '}';
}
```
</div>

اگر این شیء Log شود، رمز عبور یا Token نیز در لاگ ذخیره می‌شود که یک آسیب‌پذیری امنیتی جدی است.

### مثال Anti-Pattern شماره ۳

<div dir="ltr">

```java
User user = ...
String[] values = user.toString().split(",");  // ❌ وابسته به قالب
```
</div>

این کد با هر تغییر کوچکی در قالب `toString()` خراب خواهد شد.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

| قانون | توضیح |
|-------|-------|
| **Override کنید** | تقریباً تمام کلاس‌های قابل Instantiate باید `toString()` را Override کنند |
| **مختصر و مفید** | خروجی باید **مختصر، خوانا و معنی‌دار** باشد |
| **برای انسان است** | `toString()` برای انسان است، نه برای منطق برنامه |
| **اطلاعات حساس را حذف کنید** | Password، Secret Key، Token و داده‌های محرمانه را نمایش ندهید |
| **Format را مستند کنید** | اگر قالب رسمی است، آن را مستند کنید و سازگاری نسخه‌ها را تضمین کنید |
| **Get‌کننده داشته باشید** | هیچ‌گاه کاربران را مجبور نکنید اطلاعات را با Parse کردن `toString()` استخراج کنند |

### خلاصه نهایی

```
اگر کلاس شما:
    - قابل Instantiate است
    - منطقی است که انسان آن را ببیند
    - Utility Class یا Enum نیست

✅ حتماً toString را Override کنید
✅ محتوای مفید و مختصر ارائه دهید
✅ اطلاعات حساس را حذف کنید
✅ برای داده‌های مهم، Getter ارائه دهید
```

---

[بازگشت به بالا](#top)

</div>