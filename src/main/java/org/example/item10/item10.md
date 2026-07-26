<div dir="rtl">

<a id="top"></a>

# آیتم ۱۰: قرارداد equals را رعایت کنید (Obey the general contract of equals)

به نظر من **Item 10 یکی از بنیادی‌ترین آیتم‌های کل کتاب Effective Java** است.

اگر این آیتم را کاملاً درک کنی، حدود ۷۰٪ مشکلات مربوط به Collectionها، HashMap، HashSet، Hibernate، JPA، Cache، Distributed Cache، Kafka Keyها، Domain Modelها و Value Objectها برایت حل می‌شود.

---

## فهرست مطالب

- [معماری مسئله](#architectural-view)
  - [Identity Equality](#identity-equality)
  - [Logical Equality](#logical-equality)
- [چه زمانی equals را Override نکنیم؟](#when-not-to-override)
- [چه زمانی باید Override کنیم؟](#when-to-override)
- [قرارداد equals (۵ قانون)](#contract)
  - [۱. Reflexive](#reflexive)
  - [۲. Symmetric](#symmetric)
  - [۳. Transitive](#transitive)
  - [۴. Consistency](#consistency)
  - [۵. Non-null](#non-null)
- [instanceof یا getClass؟](#instanceof-vs-getclass)
- [الگوریتم استاندارد equals](#algorithm)
- [مقایسه انواع فیلدها](#field-comparison)
- [Production Grade Example](#production-example)
- [Anti-Patternهای رایج](#anti-patterns)
- [ارتباط با سایر Itemها](#connection-to-other-items)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="architectural-view"></a>
## معماری مسئله

قبل از اینکه درباره equals صحبت کنیم باید یک سؤال مهم را جواب بدهیم.

در Java دو نوع Equality وجود دارد.

<a id="identity-equality"></a>
### Identity Equality

```
a == b
```

یعنی:

> آیا این دو Reference دقیقاً به یک Object اشاره می‌کنند؟

مثال:

<div dir="ltr">

```java
User u1 = new User("Ali");
User u2 = u1;

System.out.println(u1 == u2); // true
```
</div>

<a id="logical-equality"></a>
### Logical Equality

گاهی اصلاً مهم نیست دو Object یکی باشند. مهم این است که **مقدارشان یکی باشد.**

مثلاً:

- `PhoneNumber`
- `Money`
- `Coordinate`
- `Email`
- `UUID`
- `DateRange`

اگر دو `PhoneNumber` شماره یکسانی داشته باشند، از نظر Domain آنها برابرند.

برای همین `equals` وجود دارد.

[بازگشت به بالا](#top)

---

<a id="when-not-to-override"></a>
## چه زمانی equals را Override نکنیم؟

کتاب دقیقاً چهار حالت معرفی می‌کند.

### ۱. Objectها ذاتاً Unique هستند

مثل:

- `Thread`
- `Socket`
- `Connection`
- `Process`

هیچکس انتظار ندارد `Thread A` با `Thread B` برابر باشد. Identity کافی است.

### ۲. Logical Equality لازم نیست

مثلاً `Pattern`. دو Regex مشابه نیازی نیست برابر باشند.

### ۳. Superclass قبلاً equals مناسبی نوشته است

مثلاً:

- `AbstractList`
- `AbstractSet`
- `AbstractMap`

### ۴. کلاس private است

هیچکس خارج کلاس آن را Compare نمی‌کند.

[بازگشت به بالا](#top)

---

<a id="when-to-override"></a>
## چه زمانی باید Override کنیم؟

هر وقت کلاس یک **Value Object** باشد.

مثلاً:

- `Money`
- `PhoneNumber`
- `Coordinate`
- `Email`
- `Currency`
- `Temperature`
- `EmployeeId`

مثال: اگر دو شیء `PhoneNumber` شماره یکسانی داشته باشند، باید برابر باشند.

[بازگشت به بالا](#top)

---

<a id="contract"></a>
## قرارداد equals (۵ قانون)

کتاب می‌گوید: اگر Override کردی، باید ۵ قانون را رعایت کنی.

<a id="reflexive"></a>
### ۱. Reflexive
<div dir="ltr">

```
x.equals(x) must be true
```
</div>
همیشه `object == itself`.

مثال:

<div dir="ltr">

```java
phone.equals(phone) // باید true باشد
```
</div>

<a id="symmetric"></a>
### ۲. Symmetric

اگر `A.equals(B)` درست است، باید `B.equals(A)` هم درست باشد.

کتاب مثال بسیار معروف `CaseInsensitiveString` را آورده:

<div dir="ltr">

```java
CaseInsensitiveString cis = new CaseInsensitiveString("Polish");
String s = "polish";
```
</div>

اگر بنویسی:

<div dir="ltr">

```java
cis.equals(s)  // true
```
</div>

ولی:

<div dir="ltr">

```java
s.equals(cis)  // false
```
</div>

تقارن شکسته شده است.

نتیجه؟ Collectionها رفتار غیرقابل پیش‌بینی پیدا می‌کنند. مثلاً `list.contains(...)` ممکن است `true`، `false` یا حتی Exception برگرداند.

<a id="transitive"></a>
### ۳. Transitive

اگر:

```
A == B
B == C
```

باشد، حتماً `A == C` هم باید باشد.

کتاب مثال معروف `Point` و `ColorPoint` را آورده.

فرض کن:

<div dir="ltr">

```java
Point
```
</div>

فقط `x` و `y` دارد. بعد `ColorPoint` می‌آید و `color` اضافه می‌کند.

اگر `Point.equals()` رنگ را نبیند ولی `ColorPoint.equals()` رنگ را بررسی کند، نتیجه:

```
A == B
B == C
ولی
A != C
```

خواهد شد. این نقض Transitivity است.

### مهم‌ترین نتیجه Item 10

کتاب می‌گوید:

> **نمی‌توان یک کلاس قابل نمونه‌سازی (instantiable) را با ارث‌بری توسعه داد و در عین حال یک مؤلفهٔ ارزشی جدید (value component) به آن افزود و همچنان قرارداد equals را حفظ کرد.**

راه‌حل؟ **Composition**.

به جای:

<div dir="ltr">

```java
ColorPoint extends Point
```
</div>

بنویس:

<div dir="ltr">

```java
class ColorPoint {
    private Point point;
    private Color color;
}
```
</div>

دقیقاً همان توصیه Item 18.

<a id="consistency"></a>
### ۴. Consistency

اگر `A.equals(B)` امروز `true` است، فردا هم باید `true` بماند، مگر اینکه State تغییر کند.

به همین دلیل `equals` نباید به چیزهایی مثل:

- Network
- Database
- Current Time
- Random
- HTTP
- DNS

وابسته باشد.

کتاب مثال `java.net.URL` را می‌زند. URL برای equals به DNS مراجعه می‌کند. DNS ممکن است تغییر کند. در نتیجه `equals` در زمان‌های مختلف جواب متفاوت می‌دهد. این طراحی **اشتباه** است.

<a id="non-null"></a>
### ۵. Non-null
<div dir="ltr">

```
object.equals(null)
```
</div>
همیشه `false` است. نه Exception.

[بازگشت به بالا](#top)

---

<a id="instanceof-vs-getclass"></a>
## instanceof یا getClass؟

یکی از معروف‌ترین بحث‌های Java.

### روش اول: instanceof

کتاب این را توصیه می‌کند:

<div dir="ltr">

```java
if (!(obj instanceof PhoneNumber))
    return false;
```
</div>

### روش دوم: getClass()

<div dir="ltr">

```java
if (obj.getClass() != getClass())
    return false;
```
</div>

کتاب می‌گوید این **Liskov Substitution Principle** را می‌شکند.

چرا؟ چون `CounterPoint extends Point` دیگر `Point` حساب نمی‌شود.

[بازگشت به بالا](#top)

---

<a id="algorithm"></a>
## الگوریتم استاندارد equals

کتاب در انتها Recipe می‌دهد:
<div dir="ltr">

```
1) if (this == o) return true;
    ↓
2) instanceof
    ↓
3) cast
    ↓
4) تمام فیلدهای مهم را Compare کن
    ↓
5) return true;
```
</div>

[بازگشت به بالا](#top)

---

<a id="field-comparison"></a>
## مقایسه انواع فیلدها

| نوع | روش |
|-----|-----|
| `int`, `long` | `==` |
| `boolean` | `==` |
| Object | `equals` |
| nullable Object | `Objects.equals` |
| `float` | `Float.compare` |
| `double` | `Double.compare` |
| Array | `Arrays.equals` |

[بازگشت به بالا](#top)

---

<a id="production-example"></a>
## Production Grade Example
<div dir="ltr">

```java
public final class Money {

    private final String currency;
    private final BigDecimal amount;

    public Money(String currency, BigDecimal amount) {
        this.currency = currency;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Money))
            return false;

        Money other = (Money) o;

        return Objects.equals(currency, other.currency)
                && Objects.equals(amount, other.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount);
    }
}
```
</div>
این دقیقاً همان الگویی است که در پروژه‌های Enterprise برای Value Objectها (مانند `Money`، `Email`، `PhoneNumber` و `OrderId`) استفاده می‌شود.

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternهای رایج

| Anti-Pattern | مشکل |
|--------------|------|
| استفاده از `==` برای Objectها | مقایسه Reference به‌جای مقدار |
| استفاده از `getClass()` بدون دلیل | نقض LSP |
| Override کردن `equals` بدون `hashCode` | خرابی `HashMap` و `HashSet` |
| وابسته بودن `equals` به Database یا Network | نقض Consistency |
| افزودن فیلد ارزشی در Subclass | نقض Transitivity |
| استفاده از پارامتر `MyClass` به‌جای `Object` | Overload شدن به‌جای Override |

[بازگشت به بالا](#top)

---

<a id="connection-to-other-items"></a>
## ارتباط Item 10 با سایر آیتم‌های کتاب

این آیتم به‌شدت با چند آیتم دیگر در ارتباط است:

| Item | ارتباط |
|------|---------|
| **Item 11** | اگر `equals` را Override کردی، باید `hashCode` را هم Override کنی؛ در غیر این صورت ساختارهایی مانند `HashMap` و `HashSet` به‌درستی کار نخواهند کرد. |
| **Item 17** | کلاس‌های Immutable بهترین گزینه برای پیاده‌سازی `equals` هستند، چون ویژگی Consistency را به‌صورت طبیعی حفظ می‌کنند. |
| **Item 18** | برای افزودن مؤلفهٔ ارزشی جدید، به‌جای ارث‌بری از Composition استفاده کن. |
| **Item 34** | `enum`ها به دلیل کنترل نمونه (Instance Control) معمولاً نیازی به Override کردن `equals` ندارند. |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

### قانون طلایی

```
اگر کلاس شما یک Value Object است:
    ✅ Override equals را به‌درستی پیاده‌سازی کنید
    ✅ ۵ قانون قرارداد را رعایت کنید
    ✅ همیشه hashCode را نیز Override کنید
    ❌ از ارث‌بری برای افزودن فیلد ارزشی استفاده نکنید
```

### خلاصه ۵ قانون

| قانون | توضیح |
|-------|-------|
| **Reflexive** | `x.equals(x)` باید `true` باشد |
| **Symmetric** | اگر `A.equals(B)` پس `B.equals(A)` |
| **Transitive** | اگر `A==B` و `B==C` پس `A==C` |
| **Consistency** | نتیجه نباید به عوامل خارجی وابسته باشد |
| **Non-null** | `x.equals(null)` باید `false` باشد |

### نکات کلیدی Production

1. **همیشه از `instanceof` استفاده کنید**، نه `getClass()`
2. **فیلدهای محاسباتی (Derived Fields)** را در `equals` بررسی نکنید
3. **فیلدهای `static`** را در `equals` بررسی نکنید
4. **فیلدهای `transient`** را در `equals` بررسی نکنید (مگر اینکه مقداردهی شده باشند)
5. **برای مقایسه `BigDecimal` از `compareTo` استفاده کنید**، نه `equals` (چون `2.0` و `2.00` با `equals` برابر نیستند)
6. **همیشه `hashCode` را با `Objects.hash()` پیاده‌سازی کنید**

[بازگشت به بالا](#top)

</div>
```