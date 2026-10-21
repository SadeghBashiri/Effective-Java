<div dir="rtl">

<a id="top"></a>

# آیتم ۳۴: به جای ثابت‌های `int` از `enum` استفاده کنید

## (Use enums instead of int constants)

این Item یکی از مهم‌ترین Items کتاب است، چون نگاه Java به `enum` را از «یک لیست از constantها» به **یک Type واقعی با behavior و invariant** تغییر می‌دهد.

---

## فهرست مطالب

- [۱. مسئله اصلی Item 34 چیست؟](#problem)
- [۲. اولین مشکل: نبود Type Safety](#no-type-safety)
- [۳. مشکل حتی بدتر: Expressionهای نامعتبر](#invalid-expressions)
- [۴. راه‌حل Java: `enum`](#solution)
- [۵. یک نکته بسیار مهم: Java Enum یک `int` نیست](#not-int)
- [۶. Enum در واقع چه ساختاری دارد؟](#structure)
- [۷. چرا Enum فقط همین instanceها را دارد؟](#instance-controlled)
- [۸. Enum و Namespace](#namespace)
- [۹. مشکل دیگری که enum حل می‌کند: Evolution](#evolution)
- [۱۰. Enum این مشکل را بهتر مدیریت می‌کند](#better-evolution)
- [۱۱. مزیت مهم: `values()`](#values)
- [۱۲. Enum دارای State](#state)
- [۱۳. چرا Fields باید `final` باشند؟](#final-fields)
- [۱۴. Enum می‌تواند Behavior داشته باشد](#behavior)
- [۱۵. مشکل اصلی Switch-on-Enum](#switch-problem)
- [۱۶. راه بهتر: Constant-Specific Method Implementation](#constant-specific)
- [۱۷. یک Design Principle مهم](#polymorphism)
- [۱۸. اما این روش همیشه بهترین نیست](#not-always-best)
- [۱۹. Strategy Enum Pattern](#strategy-enum)
- [۲۰. چه زمانی Switch خوب است؟](#when-switch)
- [۲۱. `toString()` و `valueOf()`](#tostring-valueof)
- [۲۲. اگر String representation سفارشی داریم؟](#custom-string)
- [۲۳. نکته بسیار مهم: Enum Constructor Lifecycle](#constructor-lifecycle)
- [۲۴. `Optional` در `fromString`](#optional-fromstring)
- [۲۵. یک نکته مهم در Production: `Enum.ordinal()`](#ordinal)
- [۲۶. Enum در Persistence و Distributed Systems](#persistence)
- [۲۷. Enum و JSON/API](#json-api)
- [۲۸. Enum در Architecture چه نقشی دارد؟](#architecture-role)
- [۲۹. Decision Framework](#decision-framework)
- [۳۰. مقایسه معماری](#comparison)
- [۳۱. Best Practice](#best-practice)
- [۳۲. Anti-Pattern](#anti-patterns)
- [۳۳. Performance](#performance)
- [۳۴. مهم‌ترین مفهوم Item 34](#mental-model)
- [۳۵. ارتباط Item 34 با Items قبلی](#connection)
- [۳۶. نکته بسیار مهم برای Senior/Architect](#senior-note)
- [جمع‌بندی نهایی Item 34](#final-summary)

[بازگشت به بالا](#top)

---

<a id="problem"></a>
## ۱. مسئله اصلی Item 34 چیست؟

قبل از Java 5، برای مدل‌کردن مجموعه‌ای از مقادیر ثابت معمولاً از این الگو استفاده می‌شد:

<div dir="ltr">

```java
public static final int APPLE_FUJI = 0;
public static final int APPLE_PIPPIN = 1;
public static final int APPLE_GRANNY_SMITH = 2;

public static final int ORANGE_NAVEL = 0;
public static final int ORANGE_TEMPLE = 1;
public static final int ORANGE_BLOOD = 2;
```
</div>

در ظاهر ساده است، اما از دید Type System مشکل اساسی دارد:

<div dir="ltr">

```java
void processApple(int apple) {
    // ...
}

processApple(ORANGE_NAVEL); // کاملاً legal!
```
</div>

کامپایلر نمی‌داند که `APPLE_FUJI` از نظر Domain یک `Apple` است و `ORANGE_NAVEL` یک `Orange`. هر دو فقط `int` هستند.

پس Type System هیچ اطلاعی از مفهوم Domain ندارد.

[بازگشت به بالا](#top)

---

<a id="no-type-safety"></a>
## ۲. اولین مشکل: نبود Type Safety

<div dir="ltr">

```java
public static final int APPLE_FUJI = 0;
public static final int ORANGE_NAVEL = 0;
```
</div>

حالا:

<div dir="ltr">

```java
int fruit = APPLE_FUJI;   // معتبر
int fruit = ORANGE_NAVEL; // معتبر
```
</div>

از دید compiler هیچ تفاوتی ندارند.

حتی این:

<div dir="ltr">

```java
System.out.println(APPLE_FUJI == ORANGE_NAVEL);
```
</div>

کاملاً معتبر است و نتیجه `true` است.

از دید Domain این کاملاً absurd است. ما عملاً گفته‌ایم `Apple.FUJI == Orange.NAVEL` ولی Type System قادر نیست این اشتباه را تشخیص دهد.

[بازگشت به بالا](#top)

---

<a id="invalid-expressions"></a>
## ۳. مشکل حتی بدتر: Expressionهای نامعتبر

<div dir="ltr">

```java
int i = (APPLE_FUJI - ORANGE_TEMPLE) / APPLE_PIPPIN;
```
</div>

کامپایل می‌شود. ولی معنای Domain آن چیست؟ **هیچ.**

> یک Constant صرفاً یک value نیست؛ معمولاً بخشی از یک Domain Type است.

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## ۴. راه‌حل Java: `enum`

Java 5 برای همین مشکل `enum` را معرفی کرد:

<div dir="ltr">

```java
public enum Apple {
    FUJI, PIPPIN, GRANNY_SMITH
}

public enum Orange {
    NAVEL, TEMPLE, BLOOD
}
```
</div>

حالا:

<div dir="ltr">

```java
void processApple(Apple apple) { }
```
</div>

اگر کسی بنویسد:

<div dir="ltr">

```java
processApple(Orange.NAVEL);  // ❌ Compile Error
```
</div>

کامپایلر خطا می‌دهد. یعنی `Orange ≠ Apple` و این دقیقاً همان چیزی است که Type System باید انجام دهد.

[بازگشت به بالا](#top)

---

<a id="not-int"></a>
## ۵. یک نکته بسیار مهم: Java Enum یک `int` نیست

در زبان‌هایی مثل C، enum در بسیاری از موارد اساساً حول integer values شکل گرفته است. اما در Java:

<div dir="ltr">

```java
enum Apple {
    FUJI, PIPPIN, GRANNY_SMITH
}
```
</div>

یک **class واقعی** است. یعنی:

```
enum → class-like reference type → state → behavior → methods → fields → interfaces
```

می‌توانی داشته باشی:

<div dir="ltr">

```java
public enum Apple {
    FUJI, PIPPIN, GRANNY_SMITH;

    public boolean isGreen() {
        return this == GRANNY_SMITH;
    }
}
```
</div>

و:

<div dir="ltr">

```java
Apple apple = Apple.GRANNY_SMITH;
if (apple.isGreen()) { }
```
</div>

این دیگر صرفاً Constant نیست. این یک **Domain Type** است.

[بازگشت به بالا](#top)

---

<a id="structure"></a>
## ۶. Enum در واقع چه ساختاری دارد؟

به‌صورت مفهومی:

<div dir="ltr">

```java
public enum Apple {
    FUJI, PIPPIN, GRANNY_SMITH
}
```
</div>

تقریباً می‌توانی ذهنیت زیر را داشته باشی:

<div dir="ltr">

```java
public final class Apple extends Enum<Apple> {
    public static final Apple FUJI = ...;
    public static final Apple PIPPIN = ...;
    public static final Apple GRANNY_SMITH = ...;

    private Apple(...) { }

    public static Apple[] values() { }
    public static Apple valueOf(String name) { }
}
```
</div>

البته این representation دقیق source-level نیست، اما برای درک معماری بسیار مفید است.

نتیجه:

<div dir="ltr">

```text
Apple.FUJI
Apple.PIPPIN
Apple.GRANNY_SMITH
```
</div>

instanceهای مشخصی از یک Type هستند.

[بازگشت به بالا](#top)

---

<a id="instance-controlled"></a>
## ۷. چرا Enum فقط همین instanceها را دارد؟

چون constructor قابل استفاده برای client نیست. در نتیجه `new Apple(...)` امکان‌پذیر نیست. بنابراین مجموعه instanceها کنترل‌شده است.

این همان چیزی است که در Item 3 درباره Singleton دیدیم: **Instance-Controlled Class**

- در Singleton: `1 instance`
- در Enum: `N predefined instances`

بنابراین:

<div dir="ltr">

```text
Singleton → one controlled instance
Enum → finite set of controlled instances
```
</div>

این ارتباط با Item 3 بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="namespace"></a>
## ۸. Enum و Namespace

یکی دیگر از مشکلات `int enum pattern` نبود Namespace است.

<div dir="ltr">

```java
APPLE_FUJI
ORANGE_NAVEL
```
</div>

مجبوریم Prefix استفاده کنیم. چرا؟ چون اگر بنویسیم `FUJI` و `NAVEL` ممکن است Name Collision ایجاد شود.

اما با enum:

<div dir="ltr">

```java
Apple.FUJI
Orange.NAVEL
```
</div>

هر Type Namespace خودش را دارد. بنابراین `Apple.FUJI` و `Orange.FUJI` می‌توانند همزمان وجود داشته باشند.

[بازگشت به بالا](#top)

---

<a id="evolution"></a>
## ۹. مشکل دیگری که enum حل می‌کند: Evolution

<div dir="ltr">

```java
public static final int APPLE_FUJI = 0;
```
</div>

یک `constant variable` است. Compiler می‌تواند مقدار constant را در bytecode client قرار دهد.

<div dir="ltr">

```
Library: APPLE_FUJI = 0 → Client bytecode: 0
```
</div>

اگر بعداً library تغییر کند:

<div dir="ltr">

```java
APPLE_FUJI = 10;
```
</div>

client قدیمی ممکن است همچنان `0` داشته باشد. پس:

<div dir="ltr">

```text
Library changed → Client not recompiled → Old constant value remains
```
</div>

این یک مشکل binary evolution است.

[بازگشت به بالا](#top)

---

<a id="better-evolution"></a>
## ۱۰. Enum این مشکل را بهتر مدیریت می‌کند

وقتی client می‌نویسد:

<div dir="ltr">

```java
Apple.FUJI
```
</div>

با یک semantic constant روبه‌رو هستیم، نه صرفاً یک integer literal.

> **نکته مهم:** Enum بودن به این معنی نیست که تغییرات Enum همیشه backward compatible هستند. اگر `Status.COMPLETED` را حذف کنیم و client هنوز به آن reference کند، مشکل خواهیم داشت. بنابراین Enum evolution خوب است، ولی **هر تغییر Enum بی‌خطر نیست**.

[بازگشت به بالا](#top)

---

<a id="values"></a>
## ۱۱. مزیت مهم: `values()`

Enum خودش قابلیت iteration دارد:

<div dir="ltr">

```java
for (Planet planet : Planet.values()) {
    System.out.println(planet);
}
```
</div>

در حالی که با int constants باید خودمان چنین چیزی بسازیم:

<div dir="ltr">

```java
for (int i = 0; i < NUMBER_OF_PLANETS; i++) {
    // ...
}
```
</div>

و این معمولاً brittle است.

[بازگشت به بالا](#top)

---

<a id="state"></a>
## ۱۲. Enum دارای State

> Enum می‌تواند data داشته باشد.

<div dir="ltr">

```java
public enum Planet {
    MERCURY(3.302e+23, 2.439e6),
    VENUS(4.869e+24, 6.052e6),
    EARTH(5.975e+24, 6.378e6),
    MARS(6.419e+23, 3.393e6);

    private final double mass;
    private final double radius;
    private final double surfaceGravity;

    private static final double G = 6.67300E-11;

    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
        this.surfaceGravity = G * mass / (radius * radius);
    }

    public double mass() { return mass; }
    public double radius() { return radius; }
    public double surfaceGravity() { return surfaceGravity; }

    public double surfaceWeight(double objectMass) {
        return objectMass * surfaceGravity;
    }
}
```
</div>

اینجا `Planet` تبدیل شده به یک **Immutable Domain Model**.

[بازگشت به بالا](#top)

---

<a id="final-fields"></a>
## ۱۳. چرا Fields باید `final` باشند؟

Enum instances عملاً immutable هستند. پس:

<div dir="ltr">

```java
private final double mass;
private final double radius;
private final double surfaceGravity;
```
</div>

بهترین انتخاب است. این همان **Item 17** است: Minimize mutability.

به‌خصوص در enum، mutable state معمولاً طراحی بسیار بدی است. مثلاً:

<div dir="ltr">

```java
public enum Status {
    NEW, PROCESSING;
    private int counter;

    public void increment() {
        counter++;
    }
}
```
</div>

خطرناک است چون `Status.NEW` یک shared singleton-like instance است. State آن بین تمام consumerها مشترک خواهد بود. در سیستم concurrent حتی خطرناک‌تر می‌شود.

[بازگشت به بالا](#top)

---

<a id="behavior"></a>
## ۱۴. Enum می‌تواند Behavior داشته باشد

<div dir="ltr">

```java
public enum Operation {
    PLUS, MINUS, TIMES, DIVIDE;

    public double apply(double x, double y) {
        switch (this) {
            case PLUS: return x + y;
            case MINUS: return x - y;
            case TIMES: return x * y;
            case DIVIDE: return x / y;
        }
        throw new AssertionError("Unknown operation: " + this);
    }
}
```
</div>

کار می‌کند، ولی طراحی ایده‌آلی نیست. چرا؟ چون behavior از constantها جدا شده است.

[بازگشت به بالا](#top)

---

<a id="switch-problem"></a>
## ۱۵. مشکل اصلی Switch-on-Enum

فرض کن developer یک constant اضافه کند:

<div dir="ltr">

```java
MODULO
```
</div>

ولی switch را فراموش کند.

Enum کامپایل می‌شود. اما `Operation.MODULO.apply(...)` ممکن است در runtime fail کند.

<div dir="ltr">

```text
Add enum constant → Compiler → No error → Forgot switch case → Runtime bug
```
</div>

این یک **Maintenance Hazard** است.

[بازگشت به بالا](#top)

---

<a id="constant-specific"></a>
## ۱۶. راه بهتر: Constant-Specific Method Implementation

به جای `switch (this)`، خود constant behavior خودش را تعریف می‌کند:

<div dir="ltr">

```java
public enum Operation {

    PLUS {
        @Override
        public double apply(double x, double y) { return x + y; }
    },

    MINUS {
        @Override
        public double apply(double x, double y) { return x - y; }
    },

    TIMES {
        @Override
        public double apply(double x, double y) { return x * y; }
    },

    DIVIDE {
        @Override
        public double apply(double x, double y) { return x / y; }
    };

    public abstract double apply(double x, double y);
}
```
</div>

حالا اگر `MODULO` اضافه کنیم و implementation آن را ننویسیم:

```
New enum constant → Must implement abstract method → Compiler enforces completeness
```

این بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="polymorphism"></a>
## ۱۷. یک Design Principle مهم

Constant-specific implementation عملاً نوعی **Polymorphism** است.

به جای:

<div dir="ltr">

```java
if (type == PLUS) ...
else if (type == MINUS) ...
```
</div>

می‌گوییم:

<div dir="ltr">

```text
Operation
   ├── PLUS.apply()
   ├── MINUS.apply()
   ├── TIMES.apply()
   └── DIVIDE.apply()
```
</div>

این همان حرکت از **Conditional Logic** به **Polymorphic Behavior** است.

[بازگشت به بالا](#top)

---

<a id="not-always-best"></a>
## ۱۸. اما این روش همیشه بهترین نیست

فرض کنید `PayrollDay` داریم:

```
MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
```

اما رفتار فقط دو نوع است: **WEEKDAY** و **WEEKEND**.

اگر برای هر روز behavior جدا بنویسیم، کد تکراری می‌شود.

<div dir="ltr">

```text
7 constants + 2 behaviors
```
</div>

[بازگشت به بالا](#top)

---

<a id="strategy-enum"></a>
## ۱۹. Strategy Enum Pattern

به جای اینکه behavior را مستقیماً روی `PayrollDay` قرار دهیم، یک enum داخلی برای Strategy تعریف می‌کنیم:

<div dir="ltr">

```java
private enum PayType {

    WEEKDAY {
        @Override
        int overtimePay(int minutesWorked, int payRate) {
            return minutesWorked <= MINS_PER_SHIFT
                    ? 0
                    : (minutesWorked - MINS_PER_SHIFT) * payRate / 2;
        }
    },

    WEEKEND {
        @Override
        int overtimePay(int minutesWorked, int payRate) {
            return minutesWorked * payRate / 2;
        }
    };

    abstract int overtimePay(int minutesWorked, int payRate);
}
```
</div>

سپس:

<div dir="ltr">

```java
enum PayrollDay {

    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY,

    SATURDAY(PayType.WEEKEND),
    SUNDAY(PayType.WEEKEND);

    private final PayType payType;

    PayrollDay() { this(PayType.WEEKDAY); }
    PayrollDay(PayType payType) { this.payType = payType; }

    int pay(int minutesWorked, int payRate) {
        int basePay = minutesWorked * payRate;
        return basePay + payType.overtimePay(minutesWorked, payRate);
    }
}
```
</div>

Architecture:

<div dir="ltr">

```text
PayrollDay
      ├── MONDAY ------+
      ├── TUESDAY -----+
      ├── WEDNESDAY ---+
      ├── THURSDAY ----+---- WEEKDAY Strategy
      ├── FRIDAY ------+
      ├── SATURDAY ----+---- WEEKEND Strategy
      └── SUNDAY ------+
```
</div>

از نظر Maintainability بسیار بهتر است.

[بازگشت به بالا](#top)

---

<a id="when-switch"></a>
## ۲۰. چه زمانی Switch خوب است؟

کتاب نمی‌گوید هرگز روی Enum switch نکن. بلکه می‌گوید: برای behavior ذاتی Enum، switch معمولاً انتخاب خوبی نیست.

اما اگر `Operation` را نمی‌توانید تغییر دهید و نیاز دارید یک behavior اضافی بسازید، switch منطقی است:

<div dir="ltr">

```java
public static Operation inverse(Operation op) {
    return switch (op) {
        case PLUS -> Operation.MINUS;
        case MINUS -> Operation.PLUS;
        case TIMES -> Operation.DIVIDE;
        case DIVIDE -> Operation.TIMES;
    };
}
```
</div>

Decision:

<div dir="ltr">

```text
Behavior belongs to enum? → YES → Put it inside enum
Behavior external / adapter? → YES → External switch may be appropriate
```
</div>

[بازگشت به بالا](#top)

---

<a id="tostring-valueof"></a>
## ۲۱. `toString()` و `valueOf()`

هر Enum به‌صورت خودکار دارد:

<div dir="ltr">

```java
Operation.valueOf("PLUS");   // → Operation.PLUS
Operation.PLUS.toString();   // → "PLUS"
```
</div>

اما اگر:

<div dir="ltr">

```java
@Override
public String toString() {
    return "+";
}
```
</div>

بنویسیم، `Operation.PLUS.toString()` می‌شود `"+"`. ولی `Operation.valueOf("+")` کار نمی‌کند، چون `valueOf` بر اساس **name واقعی enum constant** کار می‌کند، نه `toString()`.

[بازگشت به بالا](#top)

---

<a id="custom-string"></a>
## ۲۲. اگر String representation سفارشی داریم؟

کتاب پیشنهاد می‌کند:

<div dir="ltr">

```java
public static Optional<Operation> fromString(String symbol)
```
</div>

مثلاً:

<div dir="ltr">

```java
private static final Map<String, Operation> STRING_TO_ENUM =
        Stream.of(values())
              .collect(Collectors.toUnmodifiableMap(
                      Operation::toString,
                      Function.identity()
              ));

public static Optional<Operation> fromString(String symbol) {
    return Optional.ofNullable(STRING_TO_ENUM.get(symbol));
}
```
</div>

حالا:

<div dir="ltr">

```java
Operation.fromString("+")   // → Optional.of(Operation.PLUS)
Operation.fromString("?")   // → Optional.empty()
```
</div>

این از نظر API design بهتر از `return null;` است.

[بازگشت به بالا](#top)

---

<a id="constructor-lifecycle"></a>
## ۲۳. نکته بسیار مهم: Enum Constructor Lifecycle

این کار را نباید انجام دهیم:

<div dir="ltr">

```java
enum Operation {
    PLUS, MINUS;

    private static final Map<String, Operation> MAP = new HashMap<>();

    Operation() {
        MAP.put(name(), this);  // problematic
    }
}
```
</div>

چرا؟ ترتیب initialization مهم است:

<div dir="ltr">

```text
Create enum constants → Initialize static fields
```
</div>

پس هنگام construction constantها ممکن است static field موردنظر هنوز initialize نشده باشد. به همین دلیل map باید بعد از ساخته‌شدن constants initialize شود:

<div dir="ltr">

```java
private static final Map<String, Operation> MAP =
        Stream.of(values()).collect(...);
```
</div>

[بازگشت به بالا](#top)

---

<a id="optional-fromstring"></a>
## ۲۴. `Optional` در `fromString`

چرا `Optional<Operation>` بهتر از `Operation` است؟ چون API contract می‌گوید:

<div dir="ltr">

```text
Input → May correspond to enum → YES → Operation / NO → empty
```
</div>

در نتیجه caller مجبور است حالت invalid را در نظر بگیرد:

<div dir="ltr">

```java
Operation operation = Operation.fromString(input)
        .orElseThrow(() -> new IllegalArgumentException(
                "Unknown operation: " + input
        ));
```
</div>

این بسیار بهتر از hidden `null` contract است.

[بازگشت به بالا](#top)

---

<a id="ordinal"></a>
## ۲۵. یک نکته مهم در Production: `Enum.ordinal()`

<div dir="ltr">

```java
PLUS.ordinal()   // 0
MINUS.ordinal()  // 1
TIMES.ordinal()  // 2
```
</div>

**این کار برای business identity بسیار خطرناک است.**

اگر بعداً enum را reorder کنیم:

<div dir="ltr">

```java
enum Operation {
    PLUS, DIVIDE, MINUS, TIMES
}
```
</div>

ordinalها تغییر می‌کنند.

<div dir="ltr">

```text
ordinal ≠ stable business identifier
```
</div>

اگر persistence یا external protocol داریم:

<div dir="ltr">

```java
enum Status {
    NEW(100), PROCESSING(200), COMPLETED(300);

    private final int code;
    Status(int code) { this.code = code; }
    public int code() { return code; }
}
```
</div>

این بسیار بهتر است.

[بازگشت به بالا](#top)

---

<a id="persistence"></a>
## ۲۶. Enum در Persistence و Distributed Systems

<div dir="ltr">

```java
enum OrderStatus {
    CREATED, PAID, SHIPPED, CANCELLED
}
```
</div>

اگر `status.ordinal()` را داخل database ذخیره کنید:

<div dir="ltr">

```text
CREATED → 0 / PAID → 1 / SHIPPED → 2 / CANCELLED → 3
```
</div>

بعد developer enum را reorder کند:

<div dir="ltr">

```java
CREATED, CANCELLED, PAID, SHIPPED
```
</div>

اکنون:

<div dir="ltr">

```text
0 → CREATED / 1 → CANCELLED / 2 → PAID / 3 → SHIPPED
```
</div>

Database قدیمی دیگر semantic meaning درست ندارد. این یک **Production Anti-Pattern** است.

[بازگشت به بالا](#top)

---

<a id="json-api"></a>
## ۲۷. Enum و JSON/API

در APIها:

```json
{ "status": "PAID" }
```

معمولاً بهتر از:

```json
{ "status": 2 }
```

است.

اما حتی String enum هم باید با Contract Management درست طراحی شود. برای APIهای public معمولاً بهتر است یک **stable external code** داشته باشیم:

<div dir="ltr">

```java
public enum OrderStatus {
    CREATED("created"),
    PAID("paid"),
    SHIPPED("shipped"),
    CANCELLED("cancelled");

    private final String code;

    OrderStatus(String code) { this.code = code; }
    public String code() { return code; }
}
```
</div>

<div dir="ltr">

```text
Java identifier → CREATED
External contract → "created"
```
</div>

از هم جدا شده‌اند. این Separation بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="architecture-role"></a>
## ۲۸. Enum در Architecture چه نقشی دارد؟

Enum معمولاً برای **Closed Set** مناسب است:

> مجموعه مقادیری که Domain در زمان compile-time می‌شناسد.

مثلاً:
<div dir="ltr">

```
OrderStatus / PaymentMethod / Currency / DayOfWeek
Environment / LogLevel / Operation / UserRole
```
</div>


اما اگر مجموعه dynamic باشد:

> Admin can create new payment methods

enum مناسب نیست.

<div dir="ltr">

```java
enum PaymentMethod {
    VISA, MASTERCARD, PAYPAL  // ❌ اگر dynamic باشد
}
```
</div>

در این حالت:

<div dir="ltr">

```java
class PaymentMethod {
    private String code;
    private String displayName;
}
```
</div>

یا یک Domain Entity / Value Object مناسب‌تر است.

[بازگشت به بالا](#top)

---

<a id="decision-framework"></a>
## ۲۹. Decision Framework

سؤال معماری:

> آیا مجموعه مقادیر در compile-time محدود و شناخته‌شده است؟

- **YES** → احتمالاً `enum` انتخاب مناسبی است.
- **NO** → احتمالاً Database-backed entity / Configuration / Value Object / Registry / Plugin model مناسب‌تر است.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۳۰. مقایسه معماری

| ویژگی | `int constants` | `String constants` | `enum` | Dynamic Entity |
|--------|-----------------|--------------------|--------|----------------|
| Type Safety | ❌ | ❌ | ✅ | ✅ |
| Namespace | ❌ | ❌ | ✅ | ✅ |
| IDE support | ضعیف | متوسط | عالی | عالی |
| Iteration | دستی | دستی | `values()` | Query |
| Behavior | ضعیف | ضعیف | عالی | عالی |
| Runtime extensibility | ❌ | ❌ | ❌ | ✅ |
| Compile-time validation | ❌ | ❌ | ✅ | محدود |
| Serialization stability | ضعیف | متوسط | نیازمند طراحی | عالی با ID |
| Memory overhead | بسیار کم | کم | کم | بیشتر |
| Domain modeling | ضعیف | ضعیف | بسیار خوب | بسیار خوب |
| مناسب برای closed set | ❌ | ❌ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| مناسب برای dynamic set | ❌ | ❌ | ❌ | ⭐⭐⭐⭐⭐ |

[بازگشت به بالا](#top)

---

<a id="best-practice"></a>
## ۳۱. Best Practice

برای یک Domain enum ساده:

<div dir="ltr">

```java
public enum OrderStatus {
    CREATED, PAID, SHIPPED, CANCELLED
}
```
</div>

اگر behavior دارد:

<div dir="ltr">

```java
public enum OrderStatus {

    CREATED {
        @Override public boolean canCancel() { return true; }
    },

    PAID {
        @Override public boolean canCancel() { return true; }
    },

    SHIPPED {
        @Override public boolean canCancel() { return false; }
    },

    CANCELLED {
        @Override public boolean canCancel() { return false; }
    };

    public abstract boolean canCancel();
}
```
</div>

اما اگر behavior پیچیده شود، ممکن است Strategy یا State pattern مناسب‌تر باشد.

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## ۳۲. Anti-Pattern

**Anti-Pattern 1:**

<div dir="ltr">

```java
public static final int CREATED = 0;
public static final int PAID = 1;
public static final int SHIPPED = 2;
```
</div>

**Anti-Pattern 2:**

<div dir="ltr">

```java
switch (status) {
    case CREATED: ...
    case PAID: ...
    case SHIPPED: ...
}
```
</div>

در حالی که behavior ذاتاً متعلق به `OrderStatus` است.

**Anti-Pattern 3:**

<div dir="ltr">

```java
status.ordinal()  // برای Database / Kafka / REST / Event / External protocol
```
</div>

**Anti-Pattern 4:**

<div dir="ltr">

```java
enum PaymentMethod {
    VISA, MASTERCARD, PAYPAL  // ❌ اگر dynamic باشد
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="performance"></a>
## ۳۳. Performance

Enumها تقریباً performance مشابه int constants دارند. البته enum در مقایسه با int هزینه‌هایی برای class loading و initialization دارد. اما در applicationهای معمولی این هزینه غالباً insignificant است.

در عوض benefitهای Type Safety، Maintainability، Readability، Correctness و Domain Modeling به‌مراتب مهم‌تر هستند.

> صرفاً برای چند instruction سریع‌تر، enum را با int جایگزین نکنید مگر اینکه profiling واقعی چنین نیازی را اثبات کند.

[بازگشت به بالا](#top)

---

<a id="mental-model"></a>
## ۳۴. مهم‌ترین مفهوم Item 34

<div dir="ltr">

```text
             Constants
                 │
       ┌─────────┴─────────┐
       │                   │
   Primitive           Domain Type
     int                   │
       │                   │
 weak semantics          enum
       │                   │
       └──────────┬────────┘
                  │
          Type Safety
                  │
             State + Behavior
                  │
        ┌─────────┴─────────┐
        │                   │
    Simple enum       Rich enum
                            │
                 ┌──────────┴──────────┐
                 │                     │
          Constant-specific       Strategy enum
             behavior              pattern
```
</div>

[بازگشت به بالا](#top)

---

<a id="connection"></a>
## ۳۵. ارتباط Item 34 با Items قبلی

<div dir="ltr">

```text
Item 3  → Singleton / instance-controlled objects
   ↓
Item 17 → Minimize mutability
   ↓
Item 18 → Favor composition
   ↓
Item 30 → Generic type systems
   ↓
Item 31 → Bounded wildcards
   ↓
Item 32 → Generics + varargs
   ↓
Item 34 → Enums as real domain types
```
</div>

ارتباط Item 3 و 34 جالب است:

<div dir="ltr">

```text
Singleton → exactly one controlled instance
Enum → finite number of controlled instances
```
</div>

بنابراین Enum را نباید صرفاً «جایگزین بهتر int» در نظر گرفت.

> **Enum یک closed, type-safe, instance-controlled domain abstraction است که می‌تواند state و behavior داشته باشد.**

[بازگشت به بالا](#top)

---

<a id="senior-note"></a>
## ۳۶. نکته بسیار مهم برای Senior/Architect

در طراحی سیستم، قبل از اینکه بگویی `enum X`، این سؤال را بپرس:

### آیا Domain واقعاً Closed است؟

- `OrderStatus` اغلب closed است.
- `PaymentProvider` ممکن است open باشد.

اگر اضافه‌شدن provider بدون deploy باید ممکن باشد، enum abstraction اشتباه است.

<div dir="ltr">

```text
Compile-time known finite set → enum
Runtime extensible set → Entity / VO / Registry
```
</div>

این یکی از مهم‌ترین تصمیم‌های معماری این Item است.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی Item 34

پیام اصلی Joshua Bloch این نیست که:

> «به‌جای `int` از `enum` استفاده کن.»

پیام عمیق‌تر این است:

> **وقتی یک مفهوم Domain دارای مجموعه محدودی از مقادیر معتبر است، آن مجموعه باید یک Type واقعی در Type System باشد، نه صرفاً تعدادی عدد یا String.**

<div dir="ltr">

```text
int constants → No type safety → enum → Type safety → Namespace
    → Iteration → State → Behavior → Polymorphism
    → Maintainable domain abstraction
```
</div>

و برای Behavior:

| نیاز | انتخاب |
|------|--------|
| Simple data | enum fields |
| Different behavior per constant | constant-specific methods |
| Shared behavior among groups | strategy enum |
| Behavior external to enum | switch may be appropriate |

و مهم‌تر از همه:

<div dir="ltr">

```text
Closed set known at compile time → enum
Open/dynamic set → entity / value object / configuration / registry
```
</div>

### یک جمله Senior-level

> **از `enum` نه صرفاً به‌عنوان جایگزین امن‌تر برای constantها، بلکه به‌عنوان یک domain abstraction ایمن، immutable و instance-controlled استفاده کنید؛ behavior را وقتی متعلق به enum است داخل آن قرار دهید، وقتی behavior بین constantها متفاوت است از constant-specific implementation و وقتی چند constant رفتار مشترک دارند از Strategy Enum Pattern استفاده کنید.**

---

[بازگشت به بالا](#top)

</div>
