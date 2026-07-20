
<div dir="rtl">

<a id="top"></a>

# آیتم ۲: طراحی API برای ساخت اشیاء با پارامترهای زیاد

اگر Item 1 درباره چگونه شیء بسازیم بود، Item 2 درباره چگونه API طراحی کنیم تا ساخت شیء ایمن، خوانا و قابل توسعه باشد است.

نکته‌ای که می‌خواهم از ابتدا بگویم این است که این آیتم فقط درباره Builder Pattern نیست؛ بلکه درباره **API Design** است. Builder فقط راه‌حلی برای یک مسئله است.

---

## فهرست مطالب

- [معماری و مسئله اصلی](#architectural-view)
- [سه راهکار اصلی](#three-solutions)
  - [روش اول: Telescoping Constructor](#telescoping-constructor)
  - [روش دوم: JavaBeans Pattern](#javabeans-pattern)
  - [روش سوم: Builder Pattern](#builder-pattern)
- [Fluent API و DSL](#fluent-api)
- [Validation در Builder](#validation)
- [Default Valueها](#default-values)
- [Immutable Object نهایی](#immutable-object)
- [Trade-off و مقایسه نهایی](#trade-off)
- [Builder برای Class Hierarchy](#builder-hierarchy)
  - [مشکل Chain شکسته شده](#broken-chain)
  - [راه‌حل: Simulated Self Type](#simulated-self-type)
  - [Covariant Return Type](#covariant-return)
- [نکات پیشرفته](#advanced-tips)
  - [استفاده از EnumSet](#enumset-usage)
  - [Defensive Copy با clone](#defensive-copy)
  - [استفاده مجدد از Builder](#builder-reuse)
- [چه زمانی از Builder استفاده نکنیم](#when-not-to-use)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## معماری و مسئله اصلی

فرض کن می‌خواهی یک کلاس طراحی کنی.  
مثلاً:

<div dir="ltr">

```java
User
```
</div>
که فیلدهای زیر را دارد:

**Required (اجباری)**
- `id`
- `username`

**Optional (اختیاری)**
- `email`
- `phone`
- `address`
- `age`
- `country`
- `bio`
- `website`
- `avatar`
- ...

سؤال اصلی این است:

> API ساخت این کلاس را چگونه طراحی کنیم؟

در واقع Joshua Bloch نمی‌پرسد Builder خوب است یا نه.  
بلکه می‌پرسد:

> وقتی تعداد پارامترها زیاد شد، بهترین API برای Object Construction چیست؟

او سه راهکار را بررسی می‌کند.

[بازگشت به بالا](#top)

---

<a id="three-solutions"></a>
## سه راهکار اصلی

```
Many Constructor Parameters
│
▼
┌─────────────────────┐
│ Telescoping         │
└─────────────────────┘
│
▼
┌─────────────────────┐
│ JavaBeans           │
└─────────────────────┘
│
▼
┌─────────────────────┐
│ Builder             │
└─────────────────────┘
```

یعنی Builder بدون مقایسه با دو روش قبلی قابل درک نیست.

[بازگشت به بالا](#top)

---

<a id="telescoping-constructor"></a>
## روش اول: Telescoping Constructor

این همان Constructor Overloading است.  
مثلاً:
<div dir="ltr">

```java
User(id, username)
User(id, username, email)
User(id, username, email, phone)
User(id, username, email, phone, address)
// ...
```
</div>
### چرا اسمش Telescoping است؟

مثل تلسکوپ که لوله‌هایش داخل هم قرار می‌گیرند.  
هر constructor قبلی را صدا می‌زند.  
مثلاً:
<div dir="ltr">

```java
public User(int id, String username) {
    this(id, username, null);
}

public User(int id, String username, String email) {
    this(id, username, email, null);
}

public User(int id, String username, String email, String phone) {
    // ...
}
```
</div>
### مزایا

- ✅ Immutable
- ✅ Thread-safe
- ✅ ساده
- ✅ Object همیشه valid است.

### اما مشکل اصلی چیست؟

فرض کن:
<div dir="ltr">

```java
new User(
    10,
    "Sadegh",
    null,
    null,
    null,
    "Netherlands",
    null,
    null,
    null
);
```
</div>
الان اگر این کد را ببینی:  
آیا می‌توانی بگویی `null` شماره تلفن است؟ یا `bio`؟ یا `website`؟ تقریباً **نه**.

### مشکل دوم: پارامترهای هم‌نوع

مثلاً:
<div dir="ltr">

```java
new User(
    id,
    username,
    email,
    address,
    phone
);
```
</div>
اگر این دو را جابه‌جا کنی:

```
address ↔ phone
```

**Compiler** هیچ ایرادی نمی‌گیرد.  
چون هر دو `String` هستند. ولی برنامه اشتباه کار می‌کند. این همان چیزی است که Joshua Bloch می‌گوید:

> Bugs compile successfully.

این یکی از بدترین نوع Bugهاست.

### مثال Production

فرض کن Payment API داریم:
<div dir="ltr">

```java
Payment(
    String sourceAccount,
    String destinationAccount,
    String description,
    String currency
)
```
</div>
اشتباه:

```java
new Payment(
    "USD",
    "AccountA",
    "Salary",
    "AccountB"
);
```

- **Compiler:** ✅ OK
- **Runtime:** ❌ Disaster

### مشکل سوم: Scalability

فرض کن:
- امروز: 6 field داری
- سال بعد: 12 field
- سال بعد: 18 field

constructor‌ها انفجاری زیاد می‌شوند.

### جمع‌بندی روش اول

| مزیت | عیب |
|------|-----|
| Immutable | خوانایی پایین |
| Thread-safe | Constructor Explosion |
| Valid Object | پارامترهای هم‌نوع |
| Fast | توسعه سخت |

[بازگشت به بالا](#top)

---

<a id="javabeans-pattern"></a>
## روش دوم: JavaBeans Pattern

ایده:
1. شیء را اول بساز.
2. بعد مقداردهی کن.

مثلاً:
<div dir="ltr">
```java
User user = new User();
user.setUsername("Sadegh");
user.setEmail("...");
user.setCountry("NL");
```
</div>
ظاهرش خیلی بهتر است.

### چرا؟

چون **Named Parameter** شبیه‌سازی می‌شود.

قبلاً داشتیم:
<div dir="ltr">
```java
new User(1, "Sadegh", "...", ...)
```
</div>
الان داریم:
<div dir="ltr">

```java
user.setEmail(...)
```
</div>
کاملاً مشخص است.

### اما مشکل اصلی

Joshua Bloch می‌گوید:

> Construction is split across multiple calls.

این جمله فوق‌العاده مهم است.

قبلاً:

```
constructor → valid object
```

الان:
<div dir="ltr">

```
new User()
    ↓
setUsername()
    ↓
setEmail()
    ↓
setCountry()
    ↓
finally valid
```
</div>
یعنی بین این مراحل، شیء **ناقص** است.

### مثال
<div dir="ltr">

```java
User user = new User();
database.save(user);  // قبل از اینکه setUsername() اجرا شود
```
</div>
Database یک User ناقص ذخیره کرده.

یا:
<div dir="ltr">

```java
sendWelcomeEmail(user);  // قبل از تنظیم email
```
</div>
داخلش `user.getEmail()` را صدا بزند.  
نتیجه: `null`

یعنی شیء وارد یک وضعیت نامعتبر (Invalid State) شده است.

### مشکل بزرگ‌تر

**Immutable بودن از بین می‌رود.**

Setter یعنی:
<div dir="ltr">

```java
user.setAge(20);
// ...
user.setAge(40);
// ...
user.setAge(10);
```
</div>
شیء دائماً تغییر می‌کند.

این برای:
- Multi-thread
- Cache
- Concurrent Programming

اصلاً خوب نیست.

### مشکل Thread Safety

فرض کن:
<div dir="ltr">

- **Thread A:** `user.setName("Ali");`
- **Thread B:** `database.save(user);`
</div>
هم‌زمان اجرا شوند.  
شیء ممکن است نیمه‌کاره باشد.

به همین دلیل Joshua Bloch می‌گوید:

> JavaBeans pattern requires extra effort to make thread-safe.

### جمع‌بندی JavaBeans

| مزیت | عیب |
|------|-----|
| خوانا | Mutable |
| Named Parameters | Invalid State |
| Flexible | Thread Safety |
| ساده | Object Consistency |

[بازگشت به بالا](#top)

---

<a id="builder-pattern"></a>
## روش سوم: Builder Pattern

خوب، حالا به مهم‌ترین بخش Item 2 می‌رسیم؛ جایی که Joshua Bloch می‌گوید Builder در واقع ترکیبی از مزایای Telescoping Constructor و JavaBeans است، بدون اینکه معایب آن‌ها را داشته باشد.

ابتدا دوباره مسئله را مرور کنیم.

ما می‌خواهیم این شرایط همزمان برقرار باشد:

- ✅ Object همیشه Valid باشد.
- ✅ Object Immutable باشد.
- ✅ Client Code خوانا باشد.
- ✅ Optional Parameterها به راحتی مقداردهی شوند.

این چهار هدف، Builder را به وجود آورده‌اند.

### Builder دقیقاً چیست؟

خیلی‌ها فکر می‌کنند Builder فقط یک Pattern برای ساخت Object است.  
اما از دید معماری:

> Builder یک **Temporary Mutable Object** است که مسئول جمع‌آوری پارامترهاست و در پایان یک **Immutable Object** تولید می‌کند.

به این نمودار نگاه کن:

```
Mutable
│
▼
+-------------+
|   Builder   |
+-------------+
│
build()
│
▼
+----------------+
| Immutable Obj  |
+----------------+
```

### چرا Builder Mutable است؟

به این کد نگاه کن:
<div dir="ltr">

```java
User.Builder builder = new User.Builder(id, username);

builder.email("...");
builder.phone("...");
builder.country("NL");
```
</div>
اگر Builder هم immutable بود، هر setter باید یک Builder جدید برمی‌گرداند. یعنی:
<div dir="ltr">

```java
builder = builder.email(...);
builder = builder.phone(...);
```
</div>
خیلی غیرطبیعی می‌شد.

پس: **Builder عمداً mutable طراحی شده است.**  
اما **Object نهایی immutable است.**

### چرا Builder داخل همان کلاس قرار می‌گیرد؟

Joshua Bloch تقریباً همیشه پیشنهاد می‌کند:
<div dir="ltr">

```java
User.Builder
```
</div>
نه
<div dir="ltr">

```java
UserBuilder
```
</div>
چرا؟

زیرا **Builder بخشی از API کلاس است.**

وقتی می‌نویسی `User.Builder`، همه می‌فهمند این Builder مخصوص User است.

همچنین Builder به private constructor دسترسی دارد:
<div dir="ltr">

```java
private User(Builder builder)
```
</div>
[بازگشت به بالا](#top)

---

<a id="fluent-api"></a>
## Fluent API

این قسمت فوق‌العاده مهم است.

به این کد نگاه کن:
<div dir="ltr">

```java
new Builder(...)
    .email(...)
    .phone(...)
    .country(...)
    .build();
```
</div>
چرا این امکان وجود دارد؟

چون هر Setter این را برمی‌گرداند:
<div dir="ltr">

```java
return this;
```
</div>
مثلاً:
<div dir="ltr">

```java
public Builder email(String email) {
    this.email = email;
    return this;
}
```
</div>
اگر `void` برمی‌گرداند، دیگر نمی‌توانستیم بنویسیم:
<div dir="ltr">

```java
builder
    .email(...)
    .phone(...)
```
</div>
بلکه باید می‌نوشتیم:
<div dir="ltr">

```java
builder.email(...);
builder.phone(...);
builder.country(...);
```
</div>
این سبک را **Fluent API** می‌گویند.

### Fluent API فقط زیبایی نیست

از دید معماری:

```
Fluent API
    ↓
DSL (Domain Specific Language)
```

را ایجاد می‌کند.

مثلاً:
<div dir="ltr">

```java
HttpRequest.newBuilder()
        .uri(...)
        .header(...)
        .timeout(...)
        .build();
```
</div>
مثل یک زبان طبیعی خوانده می‌شود.

[بازگشت به بالا](#top)

---

<a id="validation"></a>
## Validation در Builder

یکی از بهترین بخش‌های این Item همین قسمت است.

Joshua Bloch می‌گوید:

> Validation را کجا انجام دهیم؟

### دو نوع Validation داریم

#### نوع اول: Validation تک‌فیلدی

مثلاً: `age > 0`

این را همان Setter Builder انجام می‌دهد:
<div dir="ltr">

```java
public Builder age(int age) {
    if (age <= 0) {
        throw new IllegalArgumentException("age must be positive");
    }
    this.age = age;
    return this;
}
```
</div>
اشتباه را همان لحظه کشف می‌کنیم.

#### نوع دوم: Validation بین چند فیلد

مثلاً: `startDate` و `endDate`  
قانون: `start < end`

این را نمی‌توان داخل Setter انجام داد.  
چرا؟ چون هنوز `endDate` تنظیم نشده است.

پس این Validation باید داخل `build()` باشد:
<div dir="ltr">

```java
public Event build() {
    if (startDate.isAfter(endDate)) {
        throw new IllegalArgumentException(
                "startDate must be before endDate"
        );
    }
    return new Event(this);
}
```
</div>
این یکی از نکات مهم طراحی Builder است.

### چرا Validation دوباره داخل Constructor انجام می‌شود؟

Joshua Bloch می‌گوید:

> حتی اگر Builder Validation انجام داد، باز هم داخل Constructor چک کن.

چرا؟

- فرض کن Builder از Reflection دستکاری شود.
- یا در آینده شخص دیگری Builder را تغییر دهد.

**Constructor آخرین خط دفاعی است.**

به همین دلیل بسیاری از کتابخانه‌های Production دوباره Validation انجام می‌دهند.

[بازگشت به بالا](#top)

---

<a id="default-values"></a>
## Default Valueها

یکی از مزیت‌های بزرگ Builder:

تمام Defaultها در یک جا قرار دارند:
<div dir="ltr">

```java
private int timeout = 30;
private boolean retry = true;
private int maxConnection = 10;
```
</div>
همه اینجا هستند.

در Constructorها، Defaultها معمولاً بین چند Constructor پخش می‌شوند.  
نگهداری سخت می‌شود.

[بازگشت به بالا](#top)

---

<a id="immutable-object"></a>
## Immutable Object نهایی

در پایان:
<div dir="ltr">

```java
private final int age;
private final String email;
```
</div>
تمام فیلدها `final` هستند.  
دیگر هیچ Setterی وجود ندارد.

این یعنی Object:

- Thread-safe
- Predictable
- Cache-friendly
- Easy to reason about

است.

[بازگشت به بالا](#top)

---

<a id="trade-off"></a>
## Trade-off و مقایسه نهایی

| ویژگی | Telescoping | JavaBeans | Builder |
|-------|-------------|-----------|---------|
| Readability | ❌ | ✅ | ✅ |
| Immutable | ✅ | ❌ | ✅ |
| Thread-safe | ✅ | ❌ | ✅ |
| Optional Params | ❌ | ✅ | ✅ |
| Object همیشه Valid | ✅ | ❌ | ✅ |
| توسعه‌پذیری | ❌ | متوسط | ✅ |

### چرا Builder فقط برای کلاس‌های بزرگ نیست؟

یک سوءبرداشت رایج این است که Builder فقط وقتی مناسب است که ۱۰ یا ۲۰ پارامتر داشته باشیم.

در عمل، در پروژه‌های Enterprise معمولاً Builder را برای کلاس‌هایی استفاده می‌کنند که:

- چند فیلد اجباری و چند فیلد اختیاری دارند.
- Immutable هستند.
- بخشی از API عمومی (Public API) محسوب می‌شوند.
- احتمال اضافه شدن فیلدهای جدید در آینده وجود دارد.

به همین دلیل است که بسیاری از APIهای مدرن جاوا مانند `HttpRequest.Builder` یا `ProcessBuilder` از همین الگو استفاده می‌کنند.

[بازگشت به بالا](#top)

---

<a id="builder-hierarchy"></a>
## Builder برای Class Hierarchy

خوب، حالا به سخت‌ترین بخش Item 2 می‌رسیم. این بخش معمولاً برای برنامه‌نویسان Mid-Level مبهم است، اما اگر آن را خوب بفهمی، درک عمیقی از طراحی APIهای Enterprise و Frameworkها پیدا می‌کنی.

تا اینجا Builder فقط برای یک کلاس بود: `User`

اما اگر ارث‌بری داشته باشیم چه؟

مثلاً:

```
               Pizza
                 ▲
        ┌────────┴────────┐
        │                 │
    NyPizza           Calzone
```

### سؤال

فرض کن می‌خواهیم Toppingها را در کلاس پایه تعریف کنیم:
<div dir="ltr">

```java
pizza.addTopping(HAM)
```
</div>
اما متدهای کلاس فرزند هم باید بعد از آن قابل فراخوانی باشند.

مثلاً:
<div dir="ltr">

```java
new Calzone.Builder()
        .addTopping(HAM)
        .sauceInside()
        .build();
```
</div>
یا:
<div dir="ltr">

```java
new NyPizza.Builder(SMALL)
        .addTopping(ONION)
        .addTopping(SAUSAGE)
        .build();
```
</div>
[بازگشت به بالا](#top)

---

<a id="broken-chain"></a>
### مشکل: Chain شکسته شده

اگر Builder ساده بنویسیم:
<div dir="ltr">

```java
class PizzaBuilder {
    PizzaBuilder addTopping(...) {
        return this;
    }
}
```
</div>
حالا `Calzone.Builder` از آن ارث می‌برد.

اما متد `addTopping()` چه چیزی برمی‌گرداند؟  
**`PizzaBuilder`**

در نتیجه:
<div dir="ltr">

```java
new Calzone.Builder()
        .addTopping(HAM)
```
</div>
نوع خروجی می‌شود: `PizzaBuilder`

پس:
<div dir="ltr">

```java
.sauceInside()
```
</div>
دیگر وجود ندارد.

یعنی **Chain شکسته می‌شود.**

### مثال بدون Generic
<div dir="ltr">

```java
PizzaBuilder builder = new Calzone.Builder();
builder.addTopping(HAM);
// دیگر نمی‌توانی:
builder.sauceInside();
```
</div>
[بازگشت به بالا](#top)

---

<a id="simulated-self-type"></a>
### راه‌حل: Simulated Self Type

Joshua Bloch از چیزی استفاده می‌کند که به آن می‌گویند:

**Simulated Self Type**

یعنی:
<div dir="ltr">

```java
abstract static class Builder<T extends Builder<T>>
```
</div>
خیلی‌ها این خط را حفظ می‌کنند.  
اما دلیلش را نمی‌فهمند.

بیایید قدم به قدم بررسی کنیم.

#### مرحله اول

فرض کن `Builder<T>` داریم.  
هنوز چیز خاصی نیست.

#### مرحله دوم

اما حالا:
<div dir="ltr">

```java
T extends Builder<T>
```
</div>
یعنی `T` خودش باید یک `Builder` باشد.

#### مرحله سوم

در کلاس فرزند:
<div dir="ltr">

```java
static class Builder
        extends Pizza.Builder<Builder>
```
</div>
حالا `T` می‌شود: `Calzone.Builder`

پس `addTopping()` دیگر `PizzaBuilder` برنمی‌گرداند.  
بلکه **`Calzone.Builder`** برمی‌گرداند.

#### نتیجه
<div dir="ltr">

```java
new Calzone.Builder()
        .addTopping(HAM)
        .sauceInside()
```
</div>
بدون Cast کار می‌کند.

### self()

Joshua Bloch این متد را اضافه می‌کند:
<div dir="ltr">

```java
protected abstract T self();
```
</div>
چرا؟

چون کلاس پایه نمی‌داند `T` واقعاً چیست.

در کلاس فرزند:
<div dir="ltr">

```java
@Override
protected Builder self() {
    return this;
}
```
</div>
حالا `addTopping()` می‌شود:
<div dir="ltr">

```java
public T addTopping(...) {
    // ...
    return self();
}
```
</div>
اگر `return this;` می‌نوشتیم، کامپایلر نمی‌توانست ثابت کند `this` از نوع `T` است.

### نتیجه

Builder پایه همیشه Builder واقعی فرزند را برمی‌گرداند.

این تکنیک در جاهای دیگر هم استفاده می‌شود:

- Query DSL
- Hibernate Criteria API
- بعضی Builderهای Spring
- بعضی Fluent APIهای AWS SDK

[بازگشت به بالا](#top)

---

<a id="covariant-return"></a>
### Covariant Return Type

حالا به قسمت بعدی می‌رسیم.

Joshua Bloch می‌گوید:
<div dir="ltr">

```java
@Override
public NyPizza build() {
    return new NyPizza(this);
}
```
</div>
سؤال: چرا `Pizza` برنگرداند؟

زیرا هر Builder باید دقیقاً همان کلاس را بسازد.

در کلاس پایه:
<div dir="ltr">

```java
abstract Pizza build();
```
</div>
اما در کلاس فرزند:
<div dir="ltr">

```java
NyPizza build();
```
</div>
این قانونی در Java است. به آن می‌گویند:

**Covariant Return Type**

یعنی Subclass می‌تواند Subtype برگرداند.

#### مثال ساده‌تر
<div dir="ltr">

```java
class Animal {
    Animal reproduce() { ... }
}

class Dog extends Animal {
    @Override
    Dog reproduce() { ... }
}
```
</div>
کاملاً قانونی است.

در Builder هم همین اتفاق می‌افتد.

### مزیت

Client دیگر Cast نمی‌کند.

بدون Covariant Return:

```java
Pizza pizza = builder.build();
NyPizza ny = (NyPizza) pizza;
```

با Covariant Return:
<div dir="ltr">

```java
NyPizza pizza = builder.build();
```
</div>
تمام.

[بازگشت به بالا](#top)

---

<a id="advanced-tips"></a>
## نکات پیشرفته

<a id="enumset-usage"></a>
### چرا EnumSet استفاده شده؟

در Builder:
<div dir="ltr">

```java
EnumSet<Topping>
```
</div>
است. چرا؟

چون `EnumSet` از نظر حافظه بسیار بهینه است.

اگر Enum کمتر از ۶۴ عضو داشته باشد، OpenJDK فقط از یک `long` استفاده می‌کند.

پس `HashSet` نباید استفاده شود.

این هم یکی از نکات Performance است که Joshua Bloch غیرمستقیم آموزش می‌دهد.

[بازگشت به بالا](#top)

<a id="defensive-copy"></a>
### clone()

Joshua Bloch می‌نویسد:
<div dir="ltr">

```java
toppings = builder.toppings.clone();
```
</div>
چرا؟

اگر بنویسد:
<div dir="ltr">

```java
toppings = builder.toppings;
```
</div>
Builder و Object هر دو به یک Set اشاره می‌کنند.

بعداً:
<div dir="ltr">

```java
builder.addTopping(...)
```
</div>
باعث تغییر Pizza هم می‌شود.

این یعنی Immutable بودن از بین رفته است.

به همین دلیل **Defensive Copy** انجام می‌شود.

#### چرا clone اینجا مجاز است؟

در کل کتاب، Joshua Bloch معمولاً توصیه می‌کند از `clone()` دوری کنیم (در Item 13 این موضوع را توضیح می‌دهد).

اما اینجا یک استثنا وجود دارد.  
دلیلش این است که `EnumSet` خودش پیاده‌سازی کنترل‌شده و بهینه‌ای از `clone()` دارد و دقیقاً برای ایجاد یک کپی مستقل طراحی شده است. بنابراین استفاده از آن هم امن است و هم سریع.

[بازگشت به بالا](#top)

<a id="builder-reuse"></a>
### استفاده مجدد از Builder

یکی از مزیت‌هایی که کمتر به آن توجه می‌شود این است که یک Builder را می‌توان چند بار استفاده کرد.

مثلاً:
<div dir="ltr">

```java
Order.Builder builder = new Order.Builder(customerId);

Order first = builder.total(100).build();
Order second = builder.total(200).build();
```
</div>
این قابلیت در سناریوهایی که بیشتر فیلدها مشترک هستند بسیار مفید است و از تکرار جلوگیری می‌کند.

[بازگشت به بالا](#top)

---

<a id="when-not-to-use"></a>
## چه زمانی از Builder استفاده نکنیم؟

خیر.  
Joshua Bloch خودش هم تأکید می‌کند که Builder هزینه دارد:

- باید یک کلاس Builder بنویسی.
- تعداد خطوط کد بیشتر می‌شود.
- برای ساخت هر شیء، ابتدا Builder ساخته می‌شود.

اگر کلاسی فقط دو یا سه پارامتر اجباری دارد و احتمال توسعه‌ی آن کم است، یک Constructor یا Static Factory کاملاً مناسب است.

اما اگر کلاس:

- پارامترهای اختیاری متعددی دارد،
- بخشی از API عمومی است،
- یا احتمال رشد آن در آینده زیاد است،

شروع کردن با Builder معمولاً تصمیم بهتری است، چون بعداً مجبور نمی‌شوی API خود را به‌صورت ناسازگار تغییر دهی.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

Joshua Bloch در این آیتم در واقع یک قانون مهم طراحی API را آموزش می‌دهد:

> Object Construction باید به گونه‌ای طراحی شود که هم برای استفاده‌کننده خوانا باشد و هم برای طراح کلاس، ایمنی و قابلیت توسعه را حفظ کند.

به همین دلیل Builder فقط یک Pattern برای ساخت شیء نیست؛ بلکه ابزاری برای طراحی:

- APIهای پایدار (Stable APIs)
- Objectهای Immutable
- کدهای قابل نگهداری در مقیاس Enterprise

این دقیقاً همان دلیلی است که بسیاری از APIهای مدرن جاوا (مانند `HttpRequest.Builder`، `UriBuilder`، AWS SDK، Elasticsearch Client و بسیاری از کتابخانه‌های دیگر) از Builder استفاده می‌کنند.

---

[بازگشت به بالا](#top)

</div>
```