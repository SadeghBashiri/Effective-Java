<div dir="rtl">

<a id="top"></a>

# آیتم ۱۳: با دقت و وسواس `clone()` را Override کنید

## Override `clone` Judiciously

> **قانون طلایی این آیتم**
>
> اگر در حال طراحی یک API جدید هستید، تقریباً هیچ‌وقت از **Cloneable** استفاده نکنید. اگر مجبور به پشتیبانی از کلاس‌های قدیمی هستید، `clone()` را مطابق قرارداد آن و با انجام **Deep Copy** برای وضعیت‌های قابل تغییر (Mutable State) پیاده‌سازی کنید. در طراحی‌های جدید، **Copy Constructor** و **Copy Factory** تقریباً همیشه انتخاب بهتری هستند.

---

## فهرست مطالب

- [بخش اول: چرا Cloneable یک Design Flaw محسوب می‌شود؟](#part1)
  - [Cloneable؛ یک Marker Interface شکست‌خورده](#cloneable-marker)
  - [نقش واقعی Cloneable چیست؟](#real-role)
  - [مکانیزمی خارج از قواعد معمول زبان](#extralinguistic)
  - [چرا این موضوع خطرناک است؟](#why-dangerous)
  - [جمع‌بندی بخش اول](#part1-summary)
- [بخش دوم: قرارداد (Contract) متد `clone()` و تفاوت Shallow Copy و Deep Copy](#part2)
  - [قرارداد (Contract) متد clone()](#contract)
  - [قانون اول: شیء Clone شده باید شیء جدیدی باشد](#rule1)
  - [قانون دوم: نوع شیء باید حفظ شود](#rule2)
  - [چرا super.clone() اهمیت دارد؟](#why-super-clone)
  - [قانون سوم: برابری منطقی](#rule3)
  - [چرا Bloch این قرارداد را ضعیف می‌داند؟](#weak-contract)
  - [متد clone در واقع یک Constructor است](#clone-as-constructor)
  - [بزرگ‌ترین دام Clone: Shallow Copy](#shallow-copy)
  - [تفاوت Shallow Copy و Deep Copy](#shallow-vs-deep)
  - [اصل طلایی Deep Copy](#golden-rule)
  - [جمع‌بندی بخش دوم](#part2-summary)
- [بخش سوم: جایگزین‌های مدرن Cloneable و جمع‌بندی نهایی](#part3)
  - [چرا Cloneable دیگر انتخاب مناسبی نیست؟](#why-not-cloneable)
  - [جایگزین اول: Copy Constructor](#copy-constructor)
  - [جایگزین دوم: Copy Factory](#copy-factory)
  - [جایگزین سوم: Builder](#builder-alternative)
  - [Interface-Based Copy](#interface-based)
  - [Collection Conversion](#collection-conversion)
  - [Clone در کلاس‌های Immutable](#clone-immutable)
  - [Clone و Thread Safety](#clone-thread-safety)
  - [چرا Frameworkهای مدرن از Cloneable استفاده نمی‌کنند؟](#modern-frameworks)
  - [چه زمانی هنوز Cloneable دیده می‌شود؟](#when-cloneable-seen)
  - [مقایسه نهایی روش‌های کپی](#final-comparison)
  - [قانون طلایی طراحی](#golden-rule-design)
  - [جمع‌بندی نهایی Item 13](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول: چرا Cloneable یک Design Flaw محسوب می‌شود؟

یکی از بحث‌برانگیزترین قابلیت‌های جاوا، مکانیزم **Cloneable** است. برخلاف بسیاری از قابلیت‌های زبان که با گذشت زمان کامل‌تر شده‌اند، Cloneable از همان نسخه‌های ابتدایی جاوا دارای مشکلات طراحی بود؛ مشکلاتی که بعدها خود **Joshua Bloch** نیز آن را یکی از بزرگ‌ترین اشتباهات طراحی در API جاوا معرفی کرد.

در نگاه اول، Cloneable ایده‌ای ساده دارد:

> «از روی یک شیء، یک کپی بساز.»

اما زمانی که وارد جزئیات پیاده‌سازی می‌شویم، با مجموعه‌ای از قراردادهای مبهم، رفتارهای غیرمعمول و محدودیت‌های معماری مواجه می‌شویم که باعث می‌شوند استفاده از آن در سیستم‌های مدرن به‌ندرت توصیه شود.

<a id="cloneable-marker"></a>
### Cloneable؛ یک Marker Interface شکست‌خورده

تعریف Cloneable تنها یک خط است:

<div dir="ltr">

```java
public interface Cloneable {
}
```
</div>

- هیچ متدی ندارد.
- هیچ قراردادی ارائه نمی‌دهد.
- هیچ رفتاری برای Client مشخص نمی‌کند.

حتی مهم‌ترین متد این مکانیزم یعنی `clone()` نیز در آن وجود ندارد. در عوض، این متد در کلاس `Object` تعریف شده است:

<div dir="ltr">

```java
protected Object clone()
```
</div>

دقت کنید: متد **protected** است، نه public.

در نتیجه صرف اینکه یک شیء Cloneable باشد، به این معنا نیست که بتوانید آن را Clone کنید.

مثلاً:

<div dir="ltr">

```java
Cloneable obj = ...;
obj.clone();      // ❌ Compile Error
```
</div>

چرا؟ چون Interface هیچ متدی به نام clone تعریف نکرده است.

<a id="real-role"></a>
### نقش واقعی Cloneable چیست؟

اینجا یکی از عجیب‌ترین قسمت‌های طراحی جاوا دیده می‌شود.

Cloneable اصلاً برای Client نوشته نشده است. بلکه تنها یک **علامت (Flag)** برای کلاس Object محسوب می‌شود.

رفتار `Object.clone()` به این صورت است:

```
implements Cloneable ?
        │
       Yes
        │
        ▼
field-by-field copy
```

اما اگر کلاس Cloneable نباشد:

```
Object.clone()
    ↓
CloneNotSupportedException
```

بنابراین وظیفه Cloneable فقط این است که رفتار یک متد protected در Superclass را تغییر دهد.

به همین دلیل Bloch می‌گوید:

> این یک استفاده‌ی کاملاً غیرمعمول (Atypical) از Interface است و نباید به عنوان الگوی طراحی از آن تقلید کرد.

<a id="extralinguistic"></a>
### مکانیزمی خارج از قواعد معمول زبان (Extralinguistic Mechanism)

یکی از مهم‌ترین جمله‌های کتاب این است:

> **Clone creates objects without calling a constructor.**

در جاوا تقریباً تمام اشیا به این شکل ساخته می‌شوند:

<div dir="ltr">

```java
new User(...)
```
</div>

که منجر به اجرای Constructor می‌شود. Constructor مسئول انجام کارهایی مانند:

- مقداردهی اولیه فیلدها
- بررسی اعتبار داده‌ها
- برقراری Invariantهای کلاس
- ثبت رخدادها (Logging)
- کنترل‌های امنیتی
- مقداردهی منابع

است. اما `super.clone()` تمام این مراحل را دور می‌زند.

#### clone چگونه شیء را می‌سازد؟

به‌صورت مفهومی:
<div dir="ltr">

```
Original Object
+----------------------+
| id = 10              |
| name = "Ali"         |
| address ----------+  |
+-------------------|--+
                    |
                    ▼
              Address Object
```
</div>
زمانی که `super.clone()` اجرا می‌شود، JVM مستقیماً یک کپی از حافظه ایجاد می‌کند:
<div dir="ltr">

```
New Object
+----------------------+
| id = 10              |
| name = "Ali"         |
| address ----------+  |
+-------------------|--+
                    |
                    ▼
              همان Address قبلی
```
</div>
بدون اینکه Constructor اجرا شود. این همان چیزی است که Bloch آن را **Extralinguistic** می‌نامد؛ یعنی مکانیزمی که خارج از قواعد معمول ساخت شیء در جاوا عمل می‌کند.

<a id="why-dangerous"></a>
### چرا این موضوع خطرناک است؟

فرض کنید Constructor کلاس شما مسئول بررسی قوانین دامنه (Business Rules) باشد:

<div dir="ltr">

```java
public class BankAccount {
    private final String iban;

    public BankAccount(String iban) {
        if (!isValid(iban))
            throw new IllegalArgumentException();
        this.iban = iban;
    }
}
```
</div>

اگر شیء از طریق Constructor ساخته شود، همیشه اعتبارسنجی انجام می‌شود.

اما هنگام Clone:

<div dir="ltr">

```java
BankAccount copy = (BankAccount) super.clone();
```
</div>

هیچ‌یک از این بررسی‌ها دوباره اجرا نمی‌شوند. در نتیجه clone ممکن است شیئی ایجاد کند که Constructor هرگز اجازه ساخت آن را نمی‌داد.

<a id="part1-summary"></a>
### جمع‌بندی بخش اول

تا اینجا متوجه شدیم که چرا Joshua Bloch از همان ابتدای آیتم ۱۳ با دیدی انتقادی به Cloneable نگاه می‌کند:

- Cloneable یک **Marker Interface** است و هیچ متدی ندارد.
- متد `clone()` در خود Interface تعریف نشده و در `Object` به‌صورت `protected` قرار دارد.
- این Interface به‌جای تعریف یک قرارداد برای Client، تنها رفتار داخلی `Object.clone()` را تغییر می‌دهد.
- `super.clone()` بدون اجرای Constructor، یک **Field-by-Field Copy** ایجاد می‌کند.
- همین ویژگی باعث می‌شود مکانیزم Cloneable شکننده، غیرمعمول و از دید معماری، خارج از الگوی طبیعی ساخت اشیا در جاوا باشد.

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم — قرارداد (Contract) متد `clone()`، نقش `super.clone()` و تفاوت Shallow Copy و Deep Copy

در بخش اول دیدیم که چرا **Cloneable** از دید Joshua Bloch یک طراحی ناموفق محسوب می‌شود. اما هنوز یک سؤال مهم باقی مانده است:

> اگر مجبور باشیم از Cloneable استفاده کنیم، دقیقاً چه قوانینی باید رعایت شوند؟

پاسخ این سؤال در **Contract متد clone()** نهفته است. اما نکته جالب اینجاست که این قرارداد، برخلاف بسیاری از قراردادهای جاوا، **بسیار ضعیف، مبهم و تا حد زیادی مبتنی بر Convention است.**

<a id="contract"></a>
### قرارداد (Contract) متد clone()

کلاس `Object` برای متد clone سه قانون کلی تعریف می‌کند:

```text
x.clone() != x
x.clone().getClass() == x.getClass()
x.clone().equals(x)
```

اما نکته بسیار مهمی که Bloch روی آن تأکید می‌کند این است که:

> **هیچ‌کدام از این سه قانون، الزام مطلق (Absolute Requirement) نیستند.**

به عبارت دیگر، این قرارداد بیشتر یک **توصیه (Convention)** است تا یک قانون قطعی که توسط کامپایلر یا JVM بررسی شود.

<a id="rule1"></a>
### قانون اول: شیء Clone شده باید شیء جدیدی باشد
<div dir="ltr">

```text
x.clone() != x
```
</div>
یعنی:

<div dir="ltr">

```java
User u1 = ...
User u2 = u1.clone();
System.out.println(u1 == u2);  // false
```
</div>

چرا؟ زیرا هدف clone ایجاد **یک شیء جدید** است، نه بازگرداندن همان Reference.

**نمایش حافظه:**

قبل از Clone:
<div dir="ltr">

```text
u1
 │
 ▼
+----------------+
| User           |
| name = Ali     |
+----------------+
```
</div>
بعد از Clone:
<div dir="ltr">

```text
u1                  u2
 │                   │
 ▼                   ▼
+------------+    +------------+
| User       |    | User       |
| name=Ali   |    | name=Ali   |
+------------+    +------------+
```
</div>
دو شیء مستقل اما با مقدار یکسان.

<a id="rule2"></a>
### قانون دوم: نوع شیء باید حفظ شود
<div dir="ltr">

```text
x.clone().getClass() == x.getClass()
```
</div>
فرض کنید:

<div dir="ltr">

```java
class Animal { }
class Dog extends Animal { }
```
</div>

اگر:

<div dir="ltr">

```java
Dog dog = ...
Animal copy = dog.clone();
```
</div>

در نهایت `copy.getClass()` نباید `Animal` باشد، بلکه باید `Dog` باشد. یعنی Clone باید **همان نوع Runtime** را برگرداند.

<a id="why-super-clone"></a>
### چرا super.clone() اهمیت دارد؟

Bloch جمله بسیار مهمی می‌گوید:

> By convention, the object returned should be obtained by calling super.clone().

فرض کنید کلاس زیر را داریم:

<div dir="ltr">

```java
class Person {
    @Override
    public Person clone() {
        return new Person();  // ❌
    }
}
```
</div>

حالا کلاس دیگری از آن ارث‌بری می‌کند:

<div dir="ltr">

```java
class Employee extends Person { }
```
</div>

حالا:

<div dir="ltr">

```java
Employee emp = new Employee();
Employee copy = (Employee) emp.clone();  // ❌ ClassCastException
```
</div>

چون داخل clone والد `return new Person()` اجرا می‌شود و سپس JVM تلاش می‌کند آن را به `Employee` تبدیل کند.

**دلیل:** اگر در یکی از کلاس‌های سلسله‌مراتب ارث‌بری، قانون `super.clone()` شکسته شود، کل زنجیره Clone خراب می‌شود.

به همین دلیل Bloch می‌گوید:

> اگر Cloneable را پیاده‌سازی می‌کنید، تقریباً همیشه باید اولین خط متد clone این باشد:

<div dir="ltr">

```java
Person copy = (Person) super.clone();
```
</div>

<a id="rule3"></a>
### قانون سوم: برابری منطقی
<div dir="ltr">

```text
x.clone().equals(x)
```
</div>
معمولاً انتظار داریم:

<div dir="ltr">

```java
Employee copy = employee.clone();
copy.equals(employee)  // true
```
</div>

اما این هم الزامی نیست. مثلاً فرض کنید کلاس شما دارای `UUID` باشد. در هنگام Clone شاید تصمیم بگیرید UUID جدید تولید کنید. در این حالت `equals()` ممکن است `false` برگرداند. بنابراین این قانون نیز فقط یک Convention است.

<a id="weak-contract"></a>
### چرا Bloch این قرارداد را ضعیف می‌داند؟

زیرا هیچ‌یک از موارد زیر توسط کامپایلر بررسی نمی‌شود:

- آیا clone باید public باشد؟ خیر.
- آیا باید super.clone() را صدا بزند؟ خیر.
- آیا باید همان کلاس را برگرداند؟ خیر.
- آیا باید equals برقرار باشد؟ خیر.

تمام این موارد فقط توصیه هستند. به همین دلیل Bloch این مکانیزم را چنین توصیف می‌کند:
<div dir="ltr">

> **Fragile, dangerous and thinly documented**
</div>
<a id="clone-as-constructor"></a>
### متد clone در واقع یک Constructor است

یکی از مهم‌ترین جملات کتاب این است:
<div dir="ltr">

> **The clone method functions as a constructor.**
</div>
وقتی Constructor اجرا می‌شود دو وظیفه اصلی دارد:

1. ایجاد شیء معتبر
2. برقراری تمام Invariantهای کلاس

مثلاً:

<div dir="ltr">

```java
public class BankAccount {
    private final String iban;

    public BankAccount(String iban) {
        if (!isValid(iban))
            throw new IllegalArgumentException();
        this.iban = iban;
    }
}
```
</div>

Constructor تضمین می‌کند هیچ شیء نامعتبری ساخته نشود. اما Clone این Constructor را اجرا نمی‌کند. در نتیجه اگر بعد از `super.clone()` حالت داخلی شیء اصلاح نشود، Clone ممکن است شیئی بسازد که هرگز نباید وجود می‌داشت.

<a id="shallow-copy"></a>
### بزرگ‌ترین دام Clone: Shallow Copy

فرض کنید:

<div dir="ltr">

```java
class Address {
    private String city;
}

class Employee implements Cloneable {
    private String name;
    private Address address;

    @Override
    public Employee clone() {
        try {
            return (Employee) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```
</div>

در نگاه اول همه چیز درست به نظر می‌رسد. اما واقعاً چه اتفاقی افتاده است؟

**قبل از Clone:**
<div dir="ltr">

```text
Employee Original
+---------------------+
| name = Ali          |
| address -----------+
+--------------------|
                     |
                     ▼
             +---------------+
             | Address       |
             | Berlin        |
             +---------------+
```
</div>
**بعد از Clone:**
<div dir="ltr">

```text
Employee Original
+---------------------+
| name = Ali          |
| address -----------+
+--------------------|
                     |
                     ▼
             +---------------+
             | Address       |
             | Berlin        |
             +---------------+
                     ▲
                     |
+--------------------|
| Employee Clone     |
| name = Ali         |
| address -----------+
+--------------------+
```
</div>
دقت کنید: دو Employee وجود دارد، اما فقط **یک Address** وجود دارد.

حالا:

<div dir="ltr">

```java
copy.getAddress().setCity("Munich");
```
</div>

نسخه اصلی نیز تغییر می‌کند:

<div dir="ltr">

```java
System.out.println(original.getAddress().getCity()); // Munich
```
</div>

در حالی که انتظار داشتیم `Berlin` باشد.

**علت اصلی:** `super.clone()` تنها یک **Field-by-Field Copy** انجام می‌دهد. برای Referenceها نیز فقط خود Reference کپی می‌شود، نه شیء پشت آن. به همین دلیل این نوع کپی را **Shallow Copy** می‌نامیم.

<a id="shallow-vs-deep"></a>
### تفاوت Shallow Copy و Deep Copy

| ویژگی | Shallow Copy | Deep Copy |
|-------|--------------|-----------|
| Primitiveها | ✅ کپی می‌شوند | ✅ کپی می‌شوند |
| Immutableها | ✅ مشکلی ندارند | ✅ کپی یا اشتراک هر دو قابل قبول است |
| Mutable Objectها | ❌ Reference مشترک | ✅ شیء جدید ساخته می‌شود |
| استقلال دو شیء | ❌ ندارد | ✅ کامل |
| Shared Mutable State | ❌ دارد | ✅ ندارد |
| مناسب سیستم‌های Enterprise | ❌ خیر | ✅ بله |

<a id="golden-rule"></a>
### اصل طلایی Deep Copy

Bloch می‌گوید:

> **شیء Clone شده باید مستقل از شیء اصلی باشد.**

یعنی:

- تغییر Clone نباید Original را تغییر دهد.
- تغییر Original نباید Clone را تغییر دهد.
- هیچ Mutable Object مشترکی نباید باقی بماند.

این دقیقاً تعریف **Deep Copy** است.

<a id="part2-summary"></a>
### جمع‌بندی بخش دوم

در این بخش با مهم‌ترین قوانین و خطرات متد `clone()` آشنا شدیم:

- قرارداد `clone()` شامل سه قانون کلی است، اما هیچ‌کدام الزام قطعی نیستند و بیشتر بر پایه Convention هستند.
- برای حفظ نوع واقعی شیء در سلسله‌مراتب ارث‌بری، فراخوانی `super.clone()` تقریباً همیشه ضروری است.
- از دید Joshua Bloch، متد `clone()` مانند یک Constructor عمل می‌کند.
- `super.clone()` تنها یک **Shallow Copy** انجام می‌دهد.
- اشتراک ناخواسته‌ی Mutable Objectها بزرگ‌ترین منبع خطا در استفاده از Cloneable است.

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم: جایگزین‌های مدرن Cloneable و جمع‌بندی نهایی

در دو بخش قبل دیدیم که چرا **Cloneable** یکی از ضعیف‌ترین طراحی‌های API در جاوا محسوب می‌شود، چگونه باید در صورت اجبار آن را پیاده‌سازی کرد، و چه مشکلاتی مانند **Shallow Copy، Shared Mutable State، ناسازگاری با final، وراثت شکننده و قرارداد ضعیف** ایجاد می‌کند.

اما مهم‌ترین بخش Item 13 در واقع این است که:

> **در طراحی‌های جدید، تقریباً هیچ دلیلی برای استفاده از Cloneable وجود ندارد.**

Bloch در انتهای آیتم عملاً توصیه می‌کند که به جای clone، از روش‌های مدرن‌تر برای کپی گرفتن از اشیاء استفاده کنیم.

<a id="why-not-cloneable"></a>
### چرا Cloneable دیگر انتخاب مناسبی نیست؟

پس از مطالعه تمام مشکلات Cloneable، یک سؤال طبیعی مطرح می‌شود:

> اگر Cloneable این‌قدر مشکل دارد، چرا اصلاً هنوز وجود دارد؟

پاسخ ساده است: زیرا **Backward Compatibility** یکی از اصول اصلی جاوا است. میلیون‌ها کلاس قدیمی (Legacy) بر پایه Cloneable نوشته شده‌اند و حذف آن باعث شکستن سازگاری نسخه‌ها می‌شود.

اما برای طراحی APIهای جدید، Joshua Bloch تقریباً همیشه گزینه‌های دیگری را پیشنهاد می‌کند.

<a id="copy-constructor"></a>
### جایگزین اول: Copy Constructor (بهترین انتخاب)

ساده‌ترین و محبوب‌ترین روش، استفاده از **Copy Constructor** است.

<div dir="ltr">

```java
public class Employee {
    private final String name;
    private final Address address;

    public Employee(Employee other) {
        this.name = other.name;
        this.address = new Address(other.address);
    }
}
```
</div>

**استفاده:**

<div dir="ltr">

```java
Employee original = ...
Employee copy = new Employee(original);
```
</div>

**مزایا:**

- ✔ خوانا
- ✔ Type-safe
- ✔ سازگار با final
- ✔ بدون Cloneable
- ✔ بدون CloneNotSupportedException
- ✔ قابل درک برای همه برنامه‌نویسان

**Deep Copy بسیار ساده‌تر است:**

<div dir="ltr">

```java
public class Address {
    private final String city;

    public Address(Address other) {
        this.city = other.city;
    }
}
```
</div>

در این حالت هر شیء کاملاً مستقل است.

<a id="copy-factory"></a>
### جایگزین دوم: Copy Factory

همان ایده Copy Constructor اما به صورت Static Factory.

<div dir="ltr">

```java
public class Order {
    public static Order copyOf(Order order) {
        return new Order(order);
    }
}
```
</div>

**استفاده:**

<div dir="ltr">

```java
Order copy = Order.copyOf(original);
// یا
Order newOrder = Order.from(oldOrder);
```
</div>

**مزایا:** برخلاف Constructor می‌تواند:

- `null` را مدیریت کند
- Cache انجام دهد
- Subtype برگرداند
- Validation انجام دهد
- Immutable Object را Reuse کند

مثال:

<div dir="ltr">

```java
public static User copyOf(User user) {
    if (user == null) return null;
    return new User(user);
}
```
</div>

**مقایسه Copy Factory vs Copy Constructor:**

| معیار | Copy Constructor | Copy Factory |
|-------|------------------|--------------|
| سادگی | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| خوانایی | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| انعطاف | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| مدیریت null | ❌ | ✅ |
| Polymorphism | ❌ | ✅ |

<a id="builder-alternative"></a>
### جایگزین سوم: Builder

برای Objectهای بزرگ بهترین انتخاب است.

فرض کنید `User` دارای `25` فیلد است. استفاده از Copy Constructor سخت می‌شود.

راه بهتر:

<div dir="ltr">

```java
User copy = User.builder(original)
        .name("Ali")
        .build();
```
</div>

یا:

<div dir="ltr">

```java
User copy = original.toBuilder()
        .name("Ali")
        .build();
```
</div>

**مزایا:**

- فقط فیلدهای موردنظر تغییر می‌کنند
- Immutable باقی می‌ماند
- خوانایی بسیار بالا

<a id="interface-based"></a>
### Interface-Based Copy

یکی از ایده‌های مدرن، تعریف یک قرارداد شفاف برای کپی گرفتن است:

<div dir="ltr">

```java
public interface Copyable<T> {
    T copy();
}
```
</div>

**پیاده‌سازی:**

<div dir="ltr">

```java
public class User implements Copyable<User> {
    @Override
    public User copy() {
        return new User(this);
    }
}
```
</div>

**استفاده:**

<div dir="ltr">

```java
User copy = user.copy();
```
</div>

**چرا این طراحی بهتر است؟**

| ویژگی | Cloneable | Copyable |
|-------|-----------|----------|
| متد دارد | ❌ | ✅ |
| Contract واضح | ❌ | ✅ |
| Type-safe | ❌ | ✅ |
| Generic | ❌ | ✅ |
| قابل فهم | ❌ | ✅ |

<a id="collection-conversion"></a>
### Collection Conversion

یکی از مزایای Copy Constructor این است که می‌تواند از Interface استفاده کند.

مثلاً:

<div dir="ltr">

```java
HashSet<String> set = new HashSet<>();
TreeSet<String> tree = new TreeSet<>(set);
```
</div>

در اینجا `HashSet` → `Collection` → `TreeSet`. Clone هرگز چنین قابلیتی ندارد.

<a id="clone-immutable"></a>
### Clone در کلاس‌های Immutable

اگر کلاس Immutable باشد:

<div dir="ltr">

```java
public final class Money {
    private final BigDecimal amount;
}
```
</div>

نوشتن clone تقریباً هیچ ارزشی ندارد. زیرا:

```
Immutable Object
    ↓
تغییر نمی‌کند
    ↓
اشتراک Reference کاملاً امن است
    ↓
نیازی به کپی نیست
```

به همین دلیل Bloch صراحتاً می‌گوید:

> **Immutable Class نباید Cloneable باشد.**

<a id="clone-thread-safety"></a>
### Clone و Thread Safety

فرض کنید:

<div dir="ltr">

```java
class UserProfile {
    private List<Role> roles;
}
```
</div>

اگر clone فقط Shallow Copy انجام دهد:
<div dir="ltr">

```
Original --------+
                 |
              List<Role>
                 |
Clone ----------+
```
</div>
دو Thread مختلف ممکن است روی یک List مشترک کار کنند. نتیجه:

- Race Condition
- Lost Update
- Data Corruption

در مقابل:

<div dir="ltr">

```java
UserProfile snapshot = profile.copy();
```
</div>

هر Thread روی نسخه خودش کار می‌کند.

<a id="modern-frameworks"></a>
### چرا Frameworkهای مدرن از Cloneable استفاده نمی‌کنند؟

در پروژه‌های واقعی Spring Boot، Quarkus، Micronaut و Jakarta EE تقریباً هیچ‌گاه Cloneable به‌عنوان راهکار اصلی کپی اشیاء دیده نمی‌شود.

به‌جای آن معمولاً یکی از الگوهای زیر استفاده می‌شود:

| روش | نمونه |
|-----|-------|
| Copy Constructor | `new Order(existing)` |
| Copy Factory | `Order.copyOf(existing)` |
| Builder | `existing.toBuilder().build()` |
| MapStruct | `mapper.copy(existing)` |
| Record | ساخت نمونه جدید از روی Record |

دلیل این انتخاب‌ها روشن است:

- قرارداد واضح‌تر
- خوانایی بیشتر
- سازگاری بهتر با Immutable Object
- عدم وابستگی به رفتار خاص `Object.clone()`
- نگهداری ساده‌تر در پروژه‌های بزرگ

<a id="when-cloneable-seen"></a>
### چه زمانی هنوز Cloneable دیده می‌شود؟

امروزه Cloneable عمدتاً در این موارد مشاهده می‌شود:

- کدهای Legacy
- برخی کلاس‌های کتابخانه استاندارد جاوا مانند `ArrayList` و `HashMap`
- آرایه‌ها (`array.clone()`)

در واقع آرایه‌ها تنها موردی هستند که خود Joshua Bloch نیز آن را استفاده مناسبی از clone می‌داند، زیرا متد `clone()` روی آرایه دقیقاً همان نوع آرایه را برمی‌گرداند و کپی آن ساده، سریع و ایمن است.

<a id="final-comparison"></a>
### مقایسه نهایی روش‌های کپی

| معیار | Cloneable | Deep Clone | Copy Constructor | Copy Factory | Builder |
|-------|-----------|------------|------------------|--------------|---------|
| سادگی | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| خوانایی | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Type-safe | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Deep Copy | دستی | دستی | ساده | ساده | ساده |
| سازگاری با final | ضعیف | ضعیف | عالی | عالی | عالی |
| وراثت | شکننده | شکننده | مناسب | مناسب | مناسب |
| مناسب API جدید | ❌ | ❌ | ✅ | ✅ | ✅ |

<a id="golden-rule-design"></a>
### قانون طلایی طراحی

می‌توان کل Item 13 را در چند قانون خلاصه کرد:

1. **برای طراحی APIهای جدید از Cloneable استفاده نکنید.**
2. اگر مجبور به پشتیبانی از کدهای قدیمی هستید، `clone()` باید ابتدا `super.clone()` را فراخوانی کند.
3. تمام وضعیت‌های قابل تغییر (Mutable State) باید به‌صورت **Deep Copy** کپی شوند.
4. `clone()` باید مانند یک Constructor رفتار کند؛ یعنی شیء جدید را بدون آسیب به شیء اصلی و با رعایت تمام Invariantها ایجاد کند.
5. از فراخوانی متدهای قابل Override درون `clone()` خودداری کنید.
6. برای طراحی‌های جدید، **Copy Constructor** و **Copy Factory** تقریباً همیشه انتخاب‌های مناسب‌تری هستند.

<a id="final-summary"></a>
### جمع‌بندی نهایی Item 13

Item 13 صرفاً درباره نحوه پیاده‌سازی `clone()` نیست؛ بلکه یک درس مهم در **طراحی API** است. Joshua Bloch نشان می‌دهد که چگونه تصمیمی که سال‌ها قبل در طراحی `Cloneable` گرفته شد، به مجموعه‌ای از مشکلات مانند قراردادهای مبهم، وابستگی به پیاده‌سازی `Object`، ناسازگاری با اصول شیءگرایی، دشواری در وراثت و مدیریت نادرست وضعیت‌های قابل تغییر منجر شده است.

پیام اصلی این آیتم این است که **کپی‌کردن اشیاء باید صریح (Explicit)، قابل فهم و ایمن باشد**. Copy Constructor، Copy Factory و در بسیاری از دامنه‌های پیچیده Builder دقیقاً این ویژگی‌ها را ارائه می‌کنند؛ در حالی که Cloneable بیشتر یک سازوکار تاریخی برای حفظ سازگاری با گذشته است تا الگویی مناسب برای طراحی نرم‌افزارهای مدرن.

به همین دلیل، در پروژه‌های Enterprise امروزی، Cloneable تنها در موارد خاص یا برای پشتیبانی از کدهای قدیمی دیده می‌شود و برای طراحی کلاس‌های جدید، استفاده از الگوهای کپی صریح و شفاف، بهترین و توصیه‌شده‌ترین رویکرد است.

---

[بازگشت به بالا](#top)

</div>
```