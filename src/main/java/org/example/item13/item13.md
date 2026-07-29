<div dir="rtl">

<a id="top"></a>

# آیتم ۱۳: با دقت Clone را Override کنید (Override clone judiciously)

تا اینجا Items 1 تا 13 را تقریباً کامل جلو آمده‌ایم. Item 13 یکی از بحث‌برانگیزترین قابلیت‌های جاوا یعنی مکانیزم `Cloneable` و مشکلات طراحی آن را بررسی می‌کند. این آیتم از نظر طراحی API و معماری نیز بسیار مهم است.

---

## فهرست مطالب

- [مقدمه: چرا Cloneable مشکل‌ساز است؟](#introduction)
- [Anti-Patternهای رایج](#anti-patterns)
  - [۱. Shallow Copy (رایج‌ترین اشتباه)](#shallow-copy)
  - [۲. Clone با Constructor (نقض قرارداد)](#clone-with-constructor)
  - [۳. Clone روی Immutable Object (بیهوده)](#clone-on-immutable)
  - [۴. Clone در کلاس‌های پیچیده (نقص در Deep Copy)](#clone-complex)
- [Best Practiceها](#best-practices)
  - [۱. Immutable Object (بدون Clone)](#immutable-example)
  - [۲. Deep Copy با Copy Constructor](#deep-copy)
  - [۳. Copy Constructor (روش پیشنهادی)](#copy-constructor)
  - [۴. Copy Factory](#copy-factory)
  - [۵. Collection Conversion](#collection-conversion)
- [مقایسه روش‌ها](#comparison)
- [نکات Production](#production-tips)
- [جمع‌بندی نهایی](#final-summary)
- [ساختار پروژه پیشنهادی](#project-structure)

[بازگشت به بالا](#top)

---

<a id="introduction"></a>
## مقدمه: چرا Cloneable مشکل‌ساز است؟

`Cloneable` در جاوا یک **Marker Interface** است (بدون متد). اگر کلاسی آن را پیاده‌سازی کند، متد `clone()` از `Object` به صورت `protected` قابل دسترسی می‌شود و اگر فراخوانی شود، یک **Shallow Copy** از شیء ایجاد می‌کند.

مشکلات اصلی `Cloneable`:

| مشکل | توضیح |
|------|-------|
| **طراحی ضعیف** | بدون `Cloneable`، `clone()` فقط `CloneNotSupportedException` پرتاب می‌کند |
| **Shallow Copy پیش‌فرض** | فیلدهای Mutable را کپی نمی‌کند |
| **عدم وجود Constructor اجباری** | راهی برای تضمین ایجاد صحیح شیء وجود ندارد |
| **زنجیره ارث‌بری شکننده** | اگر زیرکلاس `clone()` را درست پیاده‌سازی نکند، کل سلسله‌مراتب خراب می‌شود |
| **قرارداد عجیب** | نه Interface است، نه کلاس؛ یک قرارداد نامشخص |

به همین دلیل Joshua Bloch به شدت توصیه می‌کند که برای طراحی‌های جدید از `Cloneable` استفاده نکنید و به جای آن از **Copy Constructor** یا **Copy Factory** استفاده کنید.

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternهای رایج

<a id="shallow-copy"></a>
### ۱. Shallow Copy (رایج‌ترین اشتباه)

<div dir="ltr">

```java
public class Employee implements Cloneable {

    private String name;
    private Address address;

    @Override
    public Employee clone() {
        try {
            return (Employee) super.clone();  // ❌ Shallow Copy
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```
</div>

<div dir="ltr">

```java
Employee e1 = new Employee("Ali", new Address("Berlin"));
Employee e2 = e1.clone();

e2.getAddress().setCity("Munich");

System.out.println(e1.getAddress().getCity()); // ❌ Munich
```
</div>

**مشکل:** هر دو شیء `Address` مشترک است. تغییر در یکی، روی دیگری نیز تأثیر می‌گذارد.

<a id="clone-with-constructor"></a>
### ۲. Clone با Constructor (نقض قرارداد)

<div dir="ltr">

```java
@Override
public Employee clone() {
    return new Employee(name, address);  // ❌ super.clone() را رعایت نکرده
}
```
</div>

ظاهرش درست است، اما:

- `super.clone()` را رعایت نکرده
- Inheritance را می‌شکند
- زیرکلاس خراب می‌شود
- قرارداد `Cloneable` نقض می‌شود

<a id="clone-on-immutable"></a>
### ۳. Clone روی Immutable Object (بیهوده)

<div dir="ltr">

```java
public final class Money implements Cloneable {

    private final BigDecimal amount;

    @Override
    public Money clone() {
        return new Money(amount);  // ❌ کاملاً بیهوده
    }
}
```
</div>

چون `Money` اصلاً immutable است، نیازی به `clone()` ندارد. کپی کردن یک شیء غیرقابل تغییر هیچ مزیتی ندارد.

<a id="clone-complex"></a>
### ۴. Clone در کلاس‌های پیچیده (نقص در Deep Copy)

<div dir="ltr">

```java
public class ComplexObject implements Cloneable {
    private HashMap<String, List<Data>> map;  // ❌ ساختار پیچیده
    private LinkedList<Item> items;
    private int[][] matrix;
    
    @Override
    public ComplexObject clone() {
        ComplexObject copy = (ComplexObject) super.clone();
        copy.map = (HashMap) map.clone();  // ❌ فقط Shallow Copy از Map
        copy.items = (LinkedList) items.clone();  // ❌ فقط Shallow Copy از List
        copy.matrix = matrix.clone();  // ❌ فقط Shallow Copy از آرایه
        return copy;
    }
}
```
</div>

مشکل: `HashMap.clone()` فقط یک Shallow Copy است (Key و Valueها را کپی نمی‌کند). همینطور `LinkedList.clone()` و `array.clone()`.

برای Deep Copy واقعی باید:

<div dir="ltr">

```java
copy.map = new HashMap<>(map);  // ✅ Constructor جدید
copy.matrix = new int[matrix.length][];
for (int i = 0; i < matrix.length; i++) {
    copy.matrix[i] = matrix[i].clone();
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="best-practices"></a>
## Best Practiceها

<a id="immutable-example"></a>
### ۱. Immutable Object (بدون Clone)

<div dir="ltr">

```java
public final class Money {

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        this.amount = amount;
    }

    public Money(Money other) {
        this.amount = other.amount;
    }

    // Getterها...
}
```
</div>

اصلاً `Cloneable` ندارد و نیازی هم ندارد.

<a id="deep-copy"></a>
### ۲. Deep Copy با Copy Constructor

<div dir="ltr">

```java
public class Employee {

    private String name;
    private Address address;

    public Employee(Employee other) {
        this.name = other.name;
        this.address = new Address(other.address);  // ✅ Deep Copy
    }
}
```
</div>

`Address` نیز:

<div dir="ltr">

```java
public class Address {

    private String city;

    public Address(Address other) {
        this.city = other.city;  // String immutable است
    }
}
```
</div>

کاملاً مستقل است.

<a id="copy-constructor"></a>
### ۳. Copy Constructor (روش پیشنهادی)

<div dir="ltr">

```java
// Copy Constructor
Order copy = new Order(original);
```
</div>

**مزایا:**
- ساده
- واضح
- Type-safe
- بدون نیاز به `Cloneable`
- با Inheritance به خوبی کار می‌کند

<a id="copy-factory"></a>
### ۴. Copy Factory

<div dir="ltr">

```java
Order copy = Order.copyOf(original);
```
</div>

یا:

<div dir="ltr">

```java
Order newOrder = Order.from(oldOrder);
```
</div>

**مزایا:**
- انعطاف‌پذیر (می‌تواند `null` را مدیریت کند)
- می‌تواند انواع مختلف کپی را پشتیبانی کند (مثلاً `copyOf` vs `newCopy`)
- قابل گسترش است

<a id="collection-conversion"></a>
### ۵. Collection Conversion

<div dir="ltr">

```java
HashSet<String> hashSet = new HashSet<>();
// ... پر کردن

TreeSet<String> treeSet = new TreeSet<>(hashSet);  // ✅ Copy با Constructor
```
</div>

این دقیقاً همان چیزی است که Joshua Bloch پیشنهاد می‌کند. `Clone` این قابلیت را ندارد.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## مقایسه روش‌ها

| روش | Recommendation | دلیل |
|-----|----------------|------|
| `Cloneable` | ❌ فقط برای Legacy | طراحی ضعیف و شکننده |
| Shallow Clone | ❌ | Shared Mutable State |
| Deep Clone با `clone()` | ⚠️ | پیچیده و مستعد خطا |
| Copy Constructor | ✅ **بهترین انتخاب** | ساده، Type-safe، خوانا |
| Copy Factory | ✅ **بهترین انتخاب** | انعطاف‌پذیر و قابل توسعه |
| Builder از روی شیء | ✅ برای Objectهای بزرگ | مناسب Domainهای پیچیده |

[بازگشت به بالا](#top)

---

<a id="production-tips"></a>
## نکات Production

در پروژه‌های مدرن (Spring Boot، Quarkus، Micronaut و سرویس‌های سازمانی) تقریباً هیچ‌وقت از `Cloneable` استفاده نمی‌شود. معمولاً یکی از این الگوها را می‌بینی:

| روش | مثال |
|-----|-------|
| **Copy Constructor** | `new Order(existing)` |
| **Static Copy Factory** | `Order.copyOf(existing)` |
| **Builder** | `existing.toBuilder().build()` |
| **MapStruct** | `mapper.copy(existing)` |
| **Record** | `record`ها خودشان کپی می‌شوند |

### چه زمانی `Cloneable` دیده می‌شود؟

- کدهای قدیمی (Legacy)
- برخی کلاس‌های کتابخانه‌ای جاوا (مثل `ArrayList`، `HashMap`)
- موارد خاص مربوط به آرایه‌ها

### قانون طلایی برای کلاس‌های جدید

```
❌ از Cloneable استفاده نکنید
✅ از Copy Constructor استفاده کنید
✅ از Copy Factory استفاده کنید
✅ برای Objectهای پیچیده از Builder استفاده کنید
```

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

### خلاصه Item 13

قانون اصلی این آیتم را می‌توان این‌گونه خلاصه کرد:

1. **از `Cloneable` برای طراحی APIهای جدید استفاده نکن.**
2. اگر مجبور به پشتیبانی از کلاس‌های قدیمی هستی، `clone()` را با فراخوانی `super.clone()` و انجام Deep Copy برای وضعیت قابل تغییر (mutable state) پیاده‌سازی کن.
3. برای طراحی‌های جدید، **Copy Constructor** و **Copy Factory** تقریباً همیشه انتخاب بهتری هستند؛ ساده‌تر، ایمن‌تر، خواناتر و سازگارتر با اصول طراحی مدرن جاوا هستند.

### جدول نهایی

| معیار | Cloneable | Copy Constructor | Copy Factory |
|-------|-----------|------------------|--------------|
| سادگی | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Type-safe | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Deep Copy | نیاز به پیاده‌سازی | خودکار با Constructor | خودکار با Factory |
| Inheritance | شکننده | ✅ کار می‌کند | ✅ کار می‌کند |
| خوانایی | متوسط | بالا | بالا |
| مناسب برای API جدید | ❌ | ✅ | ✅ |

[بازگشت به بالا](#top)

---

<a id="project-structure"></a>
## ساختار پروژه پیشنهادی

```text
effective-java-examples
│
├── item01-static-factory
├── item02-builder
├── item03-singleton
├── item04-private-constructor
├── item05-dependency-injection
├── item06-unnecessary-objects
├── item07-memory-leaks
├── item08-finalizer-cleaner
├── item09-try-with-resources
├── item10-equals
├── item11-hashcode
├── item12-tostring
└── item13-clone
      │
      ├── common
      │     ├── Address.java
      │     └── Employee.java
      │
      ├── bad
      │     ├── shallowclone
      │     │     └── ShallowCloneExample.java
      │     ├── mutable-sharing
      │     │     └── MutableSharingExample.java
      │     ├── clone-with-constructor
      │     │     └── CloneWithConstructor.java
      │     ├── recursive-stackoverflow
      │     │     └── RecursiveClone.java
      │     └── cloneable-design
      │           └── CloneableDesignFlaw.java
      │
      └── best
            ├── immutable
            │     └── ImmutableMoney.java
            ├── deepcopy
            │     └── DeepCopyEmployee.java
            ├── copyconstructor
            │     └── CopyConstructorExample.java
            ├── copyfactory
            │     └── CopyFactoryExample.java
            └── collection-copy
                  └── CollectionCopyExample.java
```

[بازگشت به بالا](#top)

</div>
```