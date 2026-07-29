<div dir="rtl">

<a id="top"></a>

# آیتم ۲۵: فایل‌های سورس را به یک Top-Level Class محدود کنید

## (Limit source files to a single top-level class)

این Item در نگاه اول شاید بسیار ساده به نظر برسد و بسیاری از برنامه‌نویسان تصور کنند که صرفاً یک **Code Style Rule** است؛ اما در واقع، این توصیه یکی از قوانین مهم **طراحی ماژول‌ها (Modular Design)** و **مدیریت Build** در Java است.

پیام اصلی Joshua Bloch این است:

> **هر فایل `.java` فقط باید یک Top-Level Class یا Interface داشته باشد.**

دلیل این توصیه فقط خوانایی نیست؛ بلکه جلوگیری از **رفتار غیرقابل پیش‌بینی در زمان کامپایل، جلوگیری از تعریف‌های تکراری (Duplicate Definitions)، افزایش Maintainability و سازگاری Build** است.

---

## فهرست مطالب

- [Top-Level Class چیست؟](#top-level)
- [آیا Java اجازه می‌دهد چند Top-Level Class در یک فایل باشند؟](#multiple-allowed)
- [پس چرا Bloch می‌گوید این کار را انجام ندهید؟](#why-not)
- [مثال کتاب](#book-example)
- [مشکل اصلی: ترتیب کامپایل](#compilation-order)
- [چرا این اتفاق می‌افتد؟](#why-happens)
- [چرا این موضوع در پروژه‌های مدرن کمتر دیده می‌شود؟](#modern-projects)
- [اصل مهم Architectural](#architectural-principle)
- [اگر کلاس فقط متعلق به کلاس دیگری است چه؟](#belongs-to-other)
- [مزیت Static Member Class](#static-advantage)
- [ارتباط با Item 24](#connection-item24)
- [ارتباط با API Design](#api-design)
- [ارتباط با Package Design](#package-design)
- [چه زمانی Static Member Class بهتر از Top-Level است؟](#when-static-better)
- [Decision Framework](#decision-framework)
- [مقایسه گزینه‌ها](#comparison)
- [نکته Senior/Architect](#senior-note)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="top-level"></a>
## ۱. Top-Level Class چیست؟

هر کلاسی که مستقیماً در سطح فایل تعریف شود، یک **Top-Level Class** است.

مثال:

<div dir="ltr">

```java
public class User { }
```
</div>

یا حتی:

<div dir="ltr">

```java
class User { }
```
</div>

هر دو Top-Level هستند، زیرا داخل کلاس دیگری قرار ندارند.

در مقابل:

<div dir="ltr">

```java
public class User {
    static class Builder { }
}
```
</div>

`Builder` دیگر Top-Level نیست، بلکه یک **Static Member Class** است.

[بازگشت به بالا](#top)

---

<a id="multiple-allowed"></a>
## ۲. آیا Java اجازه می‌دهد چند Top-Level Class در یک فایل باشند؟

بله.

مثلاً:

<div dir="ltr">

```java
public class Main { }

class User { }

class Order { }

class Product { }
```
</div>

این کد کاملاً معتبر است.

فقط یک محدودیت وجود دارد:

- حداکثر یک کلاس `public`
- نام فایل باید با همان کلاس `public` یکسان باشد.

مثلاً: `Main.java`

[بازگشت به بالا](#top)

---

<a id="why-not"></a>
## ۳. پس چرا Bloch می‌گوید این کار را انجام ندهید؟

زیرا مشکلاتی ایجاد می‌کند که در پروژه‌های واقعی بسیار خطرناک هستند.

[بازگشت به بالا](#top)

---

<a id="book-example"></a>
## ۴. مثال کتاب

فرض کنید:

**Main.java**

<div dir="ltr">

```java
public class Main {
    public static void main(String[] args) {
        System.out.println(Utensil.NAME + Dessert.NAME);
    }
}
```
</div>

**Utensil.java**

<div dir="ltr">

```java
class Utensil {
    static final String NAME = "pan";
}

class Dessert {
    static final String NAME = "cake";
}
```
</div>

خروجی:

```
pancake
```

همه چیز درست است.

### حالا اشتباه بزرگی رخ می‌دهد

یک نفر فایل جدید می‌سازد:

**Dessert.java**

و دوباره می‌نویسد:

<div dir="ltr">

```java
class Utensil {
    static final String NAME = "pot";
}

class Dessert {
    static final String NAME = "pie";
}
```
</div>

الان دو تعریف از هر دو کلاس داریم.

[بازگشت به بالا](#top)

---

<a id="compilation-order"></a>
## ۵. مشکل اصلی: ترتیب کامپایل

حالا رفتار برنامه به ترتیب اجرای `javac` بستگی پیدا می‌کند.

مثلاً:
<div dir="ltr">

```
javac Main.java
```
</div>
خروجی:
<div dir="ltr">

```
pancake
```
</div>
اما:
<div dir="ltr">

```
javac Dessert.java Main.java
```
</div>
خروجی:
<div dir="ltr">

```
potpie
```
</div>
یعنی:

**ترتیب فایل‌هایی که به کامپایلر داده می‌شود، رفتار برنامه را تغییر می‌دهد.**

این یعنی Build دیگر **Deterministic** نیست.

[بازگشت به بالا](#top)

---

<a id="why-happens"></a>
## ۶. چرا این اتفاق می‌افتد؟

کامپایلر هنگام یافتن کلاس‌ها، فایل‌های مختلف را جستجو می‌کند.

اگر `Utensil.java` را زودتر پیدا کند:
<div dir="ltr">

```
Utensil = pan
Dessert = cake
```
</div>
استفاده می‌شود.

اگر `Dessert.java` را زودتر ببیند:
<div dir="ltr">

```
Utensil = pot
Dessert = pie
```
</div>
استفاده می‌شود.

بنابراین:
<div dir="ltr">

```
Compile Order → Class Resolution → Program Behavior
```
</div>
در یک Build سالم، چنین وابستگی‌ای نباید وجود داشته باشد.

[بازگشت به بالا](#top)

---

<a id="modern-projects"></a>
## ۷. چرا این موضوع در پروژه‌های مدرن کمتر دیده می‌شود؟

امروزه ابزارهایی مانند:

- Maven
- Gradle
- Bazel

فرآیند Build را کنترل می‌کنند.

اما قانون همچنان معتبر است، زیرا:

- IDEها
- Annotation Processorها
- Incremental Compilation
- Multi-module Buildها

همگی فرض می‌کنند هر فایل فقط یک Top-Level Type دارد.

[بازگشت به بالا](#top)

---

<a id="architectural-principle"></a>
## ۸. اصل مهم Architectural

یک فایل باید فقط مسئول تعریف یک مفهوم باشد.

این دقیقاً مشابه:

- Single Responsibility Principle
- High Cohesion

است.

به جای:
<div dir="ltr">

```
User.java
User
Order
Product
Address
```
</div>
بهتر است:
<div dir="ltr">

```
User.java
Order.java
Product.java
Address.java
```
</div>
[بازگشت به بالا](#top)

---

<a id="belongs-to-other"></a>
## ۹. اگر کلاس فقط متعلق به کلاس دیگری است چه؟

اینجا Bloch نکته مهمی می‌گوید.

اگر کلاس مستقل نیست، به جای Top-Level، از **Static Member Class** استفاده کنید.

**بد:**
<div dir="ltr">

```
HttpResponse.java
HttpResponse
Builder
```
</div>
دو کلاس Top-Level.

**بهتر:**

<div dir="ltr">

```java
public class HttpResponse {
    public static class Builder { }
}
```
</div>

زیرا:

- Builder فقط برای HttpResponse است.
- از لحاظ مفهومی مستقل نیست.
- در Namespace پروژه نیز آلودگی ایجاد نمی‌کند.

[بازگشت به بالا](#top)

---

<a id="static-advantage"></a>
## ۱۰. مزیت Static Member Class

کتاب مثال زیر را می‌زند:

<div dir="ltr">

```java
public class Test {
    private static class Utensil {
        static final String NAME = "pan";
    }

    private static class Dessert {
        static final String NAME = "cake";
    }
}
```
</div>

مزایا:

- فقط Test به آن‌ها دسترسی دارد.
- دیگر Duplicate Definition ممکن نیست.
- Namespace کوچک‌تر می‌شود.
- Encapsulation بهتر می‌شود.

[بازگشت به بالا](#top)

---

<a id="connection-item24"></a>
## ۱۱. ارتباط با Item 24

**Item 24** گفت:

> اگر کلاسی فقط برای خدمت به کلاس بیرونی است، Static Member Class باشد.

**Item 25** می‌گوید:

> اگر چنین کلاسی دارید، اصلاً آن را Top-Level نکنید.

این دو Item مکمل یکدیگر هستند.

[بازگشت به بالا](#top)

---

<a id="api-design"></a>
## ۱۲. ارتباط با API Design

فرض کنید `OrderValidator.java` فقط توسط `OrderService` استفاده می‌شود.

اگر Top-Level باشد:

<div dir="ltr">

```java
public class OrderValidator
```
</div>

تمام پروژه آن را می‌بیند. ممکن است فرد دیگری از آن استفاده کند. بعداً حذف آن **Breaking Change** ایجاد می‌کند.

اما:

<div dir="ltr">

```java
private static class OrderValidator
```
</div>

کاملاً Encapsulated است.

[بازگشت به بالا](#top)

---

<a id="package-design"></a>
## ۱۳. ارتباط با Package Design

در پروژه‌های Enterprise معمولاً ساختاری مانند این می‌بینیم:
<div dir="ltr">

```
service
    UserService.java

repository
    UserRepository.java

controller
    UserController.java

model
    User.java
```
</div>
هر فایل: یک مفهوم.

این باعث می‌شود:

- Git Merge ساده‌تر شود.
- Code Review راحت‌تر شود.
- Navigation در IDE بهتر باشد.
- Refactoring آسان‌تر شود.

[بازگشت به بالا](#top)

---

<a id="when-static-better"></a>
## ۱۴. چه زمانی Static Member Class بهتر از Top-Level است؟

فرض کنید:

<div dir="ltr">

```java
public class Email { }
```
</div>

کلاسی برای ساخت ایمیل: `Builder`

آیا Builder در جای دیگری استفاده می‌شود؟ خیر.

پس:

<div dir="ltr">

```java
public class Email {
    public static class Builder { }
}
```
</div>

طراحی بهتری است.

اما `Address` را در نظر بگیرید. آیا Address فقط متعلق به User است؟ خیر. ممکن است Customer، Supplier و Warehouse همگی Address داشته باشند.

پس `Address.java` باید Top-Level باشد.

[بازگشت به بالا](#top)

---

<a id="decision-framework"></a>
## ۱۵. Decision Framework

قبل از ایجاد یک کلاس جدید، این سه سؤال را بپرسید:

### سؤال اول

آیا این کلاس از نظر مفهومی مستقل است؟
<div dir="ltr">

```
Yes → Top-Level Class
```
</div>

### سؤال دوم

اگر مستقل نیست: آیا فقط برای کلاس بیرونی است؟
<div dir="ltr">

```
Yes → Static Member Class
```
</div>

### سؤال سوم

آیا نیاز به Instance کلاس بیرونی دارد؟
<div dir="ltr">

```
Yes → Non-static Member Class
```
</div>

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۱۶. مقایسه گزینه‌ها

| رویکرد | مزایا | معایب | Use Case |
|--------|-------|-------|----------|
| **یک Top-Level Class در هر فایل** | خوانایی بالا، Build قابل پیش‌بینی، Refactoring آسان | تعداد فایل بیشتر | تقریباً همیشه |
| **چند Top-Level Class در یک فایل** | هیچ مزیت واقعی ندارد | Duplicate Definition، وابستگی به ترتیب کامپایل، نگهداری سخت | توصیه نمی‌شود |
| **Static Member Class** | Encapsulation بهتر، Namespace کوچک‌تر، عدم وابستگی به Outer Instance | فقط برای کلاس‌های وابسته مناسب است | Builder، Helper، Internal Component |
| **Non-static Member Class** | دسترسی مستقیم به State کلاس بیرونی | Reference پنهان، احتمال Memory Leak | Iterator، Adapter |

[بازگشت به بالا](#top)

---

<a id="senior-note"></a>
## ۱۷. نکته Senior/Architect

یک توسعه‌دهنده تازه‌کار ممکن است فکر کند:

> «چند کلاس را در یک فایل می‌گذارم تا تعداد فایل‌ها کمتر شود.»

اما یک Senior Developer بیشتر به این فکر می‌کند:

- آیا Build در همه محیط‌ها رفتار یکسانی دارد؟
- آیا API من حداقل سطح دسترسی لازم را دارد؟
- آیا این کلاس واقعاً مستقل است؟
- آیا این کلاس باید بخشی از Namespace عمومی باشد؟

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

سه قانون ساده ولی بسیار مهم:

| قانون | توضیح |
|-------|-------|
| **۱. یک فایل، یک Top-Level Class** | هر فایل `.java` فقط یک Top-Level Class یا Interface داشته باشد |
| **۲. Static Member Class برای کلاس‌های وابسته** | اگر یک کلاس فقط برای یک کلاس دیگر معنا دارد، آن را به‌صورت `static member class` تعریف کنید |
| **۳. هرگز چند Top-Level در یک فایل نگذارید** | این کار می‌تواند باعث رفتار وابسته به ترتیب کامپایل، مشکلات نگهداری و API نامناسب شود |

### قانون طلایی

در واقع، Item 25 ادامه طبیعی Item 24 است:

- **Item 24** می‌گوید چگونه کلاس‌های تو‌در‌تو را درست انتخاب کنید.
- **Item 25** می‌گوید اگر کلاسی واقعاً مستقل است، آن را در **فایل مستقل** قرار دهید؛ و اگر مستقل نیست، آن را به‌عنوان **Static Member Class** درون کلاس مالک نگه دارید.

این دو Item در کنار هم یک اصل مهم طراحی را شکل می‌دهند:

> **ساختار فایل‌ها باید ساختار مفهومی مدل دامنه (Domain Model) و مالکیت واقعی کلاس‌ها را منعکس کند، نه صرفاً سلیقه یا راحتی برنامه‌نویس.**

---

[بازگشت به بالا](#top)

</div>
```