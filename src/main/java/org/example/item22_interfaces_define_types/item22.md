<div dir="rtl">

<a id="top"></a>

# آیتم ۲۲: از Interface فقط برای تعریف Type استفاده کنید

## (Use interfaces only to define types)

این Item یکی از نکات مهم در طراحی API و Object-Oriented Design در Java است. Joshua Bloch در این بخش یک سوءاستفاده بسیار رایج از Interface را بررسی می‌کند:

> استفاده از Interface فقط به‌عنوان محلی برای نگهداری Constantها

که به آن **Constant Interface Pattern** می‌گویند.

این الگو در ظاهر ساده و راحت است، اما از دید طراحی نرم‌افزار یک Anti-Pattern محسوب می‌شود.

---

## فهرست مطالب

- [مفهوم اصلی Item 22](#core-concept)
- [Constant Interface Pattern چیست؟](#constant-interface)
- [مشکل اول: نشت Implementation Detail به API](#problem1)
- [مشکل دوم: ایجاد تعهد دائمی (API Commitment)](#problem2)
- [مشکل سوم: Namespace Pollution](#problem3)
- [چرا Java خودش Constant Interface دارد؟](#java-example)
- [راهکارهای درست Export کردن Constantها](#solutions)
  - [گزینه ۱: اگر Constant متعلق به یک Class است](#option1)
  - [گزینه ۲: اگر Constantها یک مفهوم مستقل هستند → Enum](#option2)
  - [گزینه ۳: Utility Class](#option3)
- [چرا Underscore در Numeric Literal؟](#underscore)
- [Static Import برای حل مشکل Verbose شدن](#static-import)
- [مقایسه رویکردها](#comparison)
- [مثال Production-Level](#production-example)
- [ارتباط با Architecture و Microservices](#architecture)
- [نکته Senior Level](#senior-note)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-concept"></a>
## ۱. مفهوم اصلی Item 22

ابتدا باید بفهمیم Interface در Java برای چه چیزی ساخته شده است.

یک Interface باید یک **Contract** یا **Type Definition** باشد.

یعنی وقتی یک کلاس Interface را implement می‌کند، باید یک معنی واقعی داشته باشد:

<div dir="ltr">

```java
interface PaymentProcessor {
    void process(Payment payment);
}
```
</div>

وقتی می‌گوییم:

<div dir="ltr">

```java
class PaypalProcessor implements PaymentProcessor
```
</div>

معنی دارد: "PaypalProcessor یک نوع PaymentProcessor است."

پس Client می‌تواند بگوید:

<div dir="ltr">

```java
PaymentProcessor processor = new PaypalProcessor();
```
</div>

و با آن کار کند.

اما مشکل زمانی ایجاد می‌شود که Interface هیچ رفتار یا Contractی ندارد:

<div dir="ltr">

```java
public interface PhysicalConstants {
    double AVOGADROS_NUMBER = 6.022e23;
    double BOLTZMANN_CONSTANT = 1.380e-23;
}
```
</div>

این Interface هیچ Type جدیدی تعریف نمی‌کند. در واقع فقط یک Container برای Constantها است.

[بازگشت به بالا](#top)

---

<a id="constant-interface"></a>
## ۲. Constant Interface Pattern چیست؟

مثال کتاب:

<div dir="ltr">

```java
public interface PhysicalConstants {
    static final double AVOGADROS_NUMBER = 6.022_140_857e23;
    static final double BOLTZMANN_CONSTANT = 1.380_648_52e-23;
    static final double ELECTRON_MASS = 9.109_383_56e-31;
}
```
</div>

بعد Developer ممکن است بنویسد:

<div dir="ltr">

```java
public class ChemistryCalculator implements PhysicalConstants {
    public double calculate(double mol) {
        return mol * AVOGADROS_NUMBER;
    }
}
```
</div>

هدف Developer: اینکه مجبور نباشد بنویسد:

<div dir="ltr">

```java
PhysicalConstants.AVOGADROS_NUMBER
```
</div>

ولی این کار چندین مشکل معماری ایجاد می‌کند.

[بازگشت به بالا](#top)

---

<a id="problem1"></a>
## ۳. مشکل اول: نشت Implementation Detail به API

مهم‌ترین نکته کتاب همین است.

فرض کنید:

<div dir="ltr">

```java
public class ChemistryCalculator implements PhysicalConstants { }
```
</div>

از دید مشتری این کلاس چه معنی دارد؟ او می‌بیند:

<div dir="ltr">

```java
ChemistryCalculator implements PhysicalConstants
```
</div>

پس فکر می‌کند: "ChemistryCalculator یک PhysicalConstants است."

اما واقعیت چیست؟ واقعیت: ChemistryCalculator فقط برای محاسبات داخلی به چند مقدار ثابت نیاز دارد.

یعنی: **Internal Detail به Public API نشت پیدا کرده است.**

در طراحی خوب:
<div dir="ltr">

```
Client → ChemistryCalculator (Implementation hidden)
```
</div>
ولی با Constant Interface:
<div dir="ltr">

```
Client → ChemistryCalculator → implements → PhysicalConstants
```
</div>
جزئیات داخلی بیرون آمده است.

[بازگشت به بالا](#top)

---

<a id="problem2"></a>
## ۴. مشکل دوم: ایجاد تعهد دائمی (API Commitment)

فرض کنید امروز:

<div dir="ltr">

```java
class ChemistryCalculator implements PhysicalConstants { }
```
</div>

فردا نسخه جدید:

<div dir="ltr">

```java
class ChemistryCalculator { }
```
</div>

دیگر به Constantها نیاز ندارد. اما حذف کردن `implements PhysicalConstants` ممکن است باعث شکستن Binary Compatibility شود. یعنی Libraryهایی که قبلاً Compile شده‌اند ممکن است مشکل پیدا کنند. پس مجبور می‌شوید چیزی را نگه دارید که دیگر نیاز ندارید.

این یک قانون مهم API Design است:

> هر چیزی که وارد Public API شود، هزینه نگهداری بلندمدت دارد.

[بازگشت به بالا](#top)

---

<a id="problem3"></a>
## ۵. مشکل سوم: Namespace Pollution

فرض کنید:

<div dir="ltr">

```java
public interface Constants {
    int MAX = 100;
    int MIN = 0;
}
```
</div>

حالا:

<div dir="ltr">

```java
public class BaseCalculator implements Constants { }
```
</div>

حالا تمام Subclassها:

<div dir="ltr">

```java
public class AdvancedCalculator extends BaseCalculator { }
```
</div>

به صورت غیر مستقیم این‌ها را دارند: `MAX`، `MIN`

یعنی Namespace آنها آلوده شده است.

مثلاً:

<div dir="ltr">

```java
public class AdvancedCalculator {
    int MAX = 500;  // ممکن است Conflict ایجاد شود
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="java-example"></a>
## ۶. چرا Java خودش Constant Interface دارد؟

کتاب اشاره می‌کند: مثلاً `java.io.ObjectStreamConstants` وجود دارد.

اما Bloch می‌گوید:

> این‌ها Anomaly هستند، تقلید نکنید.

یعنی: وجود داشتن یک Design Mistake در Java Library دلیل خوب بودن آن نیست.

[بازگشت به بالا](#top)

---

<a id="solutions"></a>
## ۷. راهکارهای درست Export کردن Constantها

کتاب سه راه پیشنهاد می‌کند.

<a id="option1"></a>
### گزینه ۱: اگر Constant متعلق به یک Class است

مثلاً:

<div dir="ltr">

```java
Integer.MAX_VALUE
```
</div>

کاملاً منطقی است. چون `MAX_VALUE` واقعاً متعلق به Integer است.

<div dir="ltr">

```java
public final class User {
    public static final int MAX_NAME_LENGTH = 50;
}
```
</div>

استفاده:

<div dir="ltr">

```java
User.MAX_NAME_LENGTH
```
</div>

معنی دارد.

<a id="option2"></a>
### گزینه ۲: اگر Constantها یک مفهوم مستقل هستند → Enum

مثلاً:

<div dir="ltr">

```java
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}
```
</div>

به جای:

<div dir="ltr">

```java
public interface PaymentConstants {
    int SUCCESS = 1;
    int FAILED = 2;
}
```
</div>

چرا؟ چون Enum یک Type واقعی می‌سازد.

<div dir="ltr">

```java
PaymentStatus status;
```
</div>

کامپایلر هم کمک می‌کند. ولی:

<div dir="ltr">

```java
int status;
```
</div>

هیچ Semantic ندارد.

<a id="option3"></a>
### گزینه ۳: Utility Class

اگر فقط مجموعه‌ای از Constantها دارید، بهترین گزینه:

<div dir="ltr">

```java
public final class PhysicalConstants {
    private PhysicalConstants() { }

    public static final double AVOGADROS_NUMBER = 6.022_140_857e23;
    public static final double BOLTZMANN_CONSTANT = 1.380_648_52e-23;
}
```
</div>

چرا `private PhysicalConstants()`؟ چون این کلاس نباید ساخته شود:

<div dir="ltr">

```java
new PhysicalConstants();  // معنی ندارد
```
</div>

[بازگشت به بالا](#top)

---

<a id="underscore"></a>
## ۸. چرا Underscore در Numeric Literal؟

کتاب اشاره می‌کند: از Java 7، `6.022_140_857e23` مجاز است. هدف: خوانایی.

مقایسه:

- بد: `602214085700000000000000`
- خوب: `602_214_085_700_000_000_000_000`

Rule پیشنهادی: برای اعداد بزرگ، هر سه رقم یک underscore: `1_000_000`

[بازگشت به بالا](#top)

---

<a id="static-import"></a>
## ۹. Static Import برای حل مشکل Verbose شدن

ممکن است بگویید: "خب `PhysicalConstants.AVOGADROS_NUMBER` طولانی است."

Java راه حل دارد: **Static Import**

<div dir="ltr">

```java
import static com.example.PhysicalConstants.*;
```
</div>

بعد:

<div dir="ltr">

```java
double result = AVOGADROS_NUMBER * mols;
```
</div>

اما نکته مهم: Static Import باید محدود استفاده شود.

مثلاً:

- خوب: `Math.PI`، `TimeUnit.SECONDS`
- بد: `import static com.company.constants.*;` در پروژه بزرگ ممکن است خوانایی را خراب کند.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۱۰. مقایسه رویکردها

| Approach | Type ایجاد می‌کند؟ | Maintainability | API Pollution | پیشنهاد |
|----------|-------------------|-----------------|---------------|---------|
| Constant Interface | ❌ | پایین | زیاد | ❌ |
| Utility Class | ❌ ولی مناسب Constant | خوب | کم | ✅ |
| Enum | ✅ | عالی | کم | ✅ برای مفهوم‌های محدود |
| Class Constants | بستگی دارد | عالی | کم | ✅ |

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## ۱۱. مثال Production-Level

فرض کنید سیستم پرداخت دارید.

### اشتباه:

<div dir="ltr">

```java
public interface PaymentConstants {
    int MAX_RETRY = 3;
}

public class PaymentService implements PaymentConstants {
    public void retry() {
        for (int i = 0; i < MAX_RETRY; i++) {
            // ...
        }
    }
}
```
</div>

مشکل: PaymentService تبدیل شده به `PaymentService → implements → PaymentConstants` در حالی که رابطه‌ای وجود ندارد.

### طراحی بهتر:

<div dir="ltr">

```java
public final class PaymentConfig {
    private PaymentConfig() { }

    public static final int MAX_RETRY = 3;
}
```
</div>

استفاده:

<div dir="ltr">

```java
if (attempt < PaymentConfig.MAX_RETRY) { }
```
</div>

واضح است.

[بازگشت به بالا](#top)

---

<a id="architecture"></a>
## ۱۲. ارتباط با Architecture و Microservices

در سیستم‌های Enterprise این مسئله مهم‌تر می‌شود.

فرض کنید:

<div dir="ltr">

```java
interface KafkaConstants { }
```
</div>

و:

<div dir="ltr">

```java
OrderService implements KafkaConstants
```
</div>

این یعنی: "OrderService is a KafkaConstants" که کاملاً غلط است.

در معماری Microservice، Configuration باید جدا باشد:

<div dir="ltr">

```java
@ConfigurationProperties("kafka")
public class KafkaProperties {
    private int retryCount;
}
```
</div>

نه Interface Constant.

[بازگشت به بالا](#top)

---

<a id="senior-note"></a>
## ۱۳. نکته Senior Level

یک Developer معمولی می‌گوید: "این روش راحت‌تر است، کمتر تایپ می‌کنم."

یک Senior Developer می‌پرسد: "آیا این تصمیم بخشی از Public API من می‌شود؟"

چون:

```
Easy today → Maintenance cost tomorrow
```

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

قانون اصلی:

> **Interface باید یک Type تعریف کند، نه یک Namespace برای Constantها.**

### ❌ اشتباه:

<div dir="ltr">

```java
public interface Constants {
    int MAX = 10;
}
```
</div>

### ✅ درست:

<div dir="ltr">

```java
public final class Constants {
    private Constants() { }

    public static final int MAX = 10;
}
```
</div>

یا:

<div dir="ltr">

```java
public enum Status {
    ACTIVE,
    INACTIVE
}
```
</div>

یا:

<div dir="ltr">

```java
class User {
    public static final int MAX_LENGTH = 50;
}
```
</div>

### جدول خلاصه

| قانون | توضیح |
|-------|-------|
| **Interface برای Type است** | هر Interface باید یک قرارداد رفتاری معنی‌دار تعریف کند |
| **Constant Interface ممنوع** | هرگز از Interface فقط برای نگهداری Constantها استفاده نکنید |
| **از Utility Class برای Constantها استفاده کنید** | با Constructor خصوصی و فیلدهای `public static final` |
| **از Enum برای مجموعه‌های محدود استفاده کنید** | به‌جای اعداد جادویی یا کدهای عددی |
| **Static Import را محدود استفاده کنید** | خوانایی کد را فدای اختصار نکنید |

---

> این Item در واقع ادامه فلسفه کلی Effective Java است: **طراحی API باید فقط چیزی را expose کند که واقعاً بخشی از Contract کلاس است، نه جزئیات داخلی.**

[بازگشت به بالا](#top)

</div>
```