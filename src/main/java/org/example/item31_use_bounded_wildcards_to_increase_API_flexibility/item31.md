<div dir="rtl">

<a id="top"></a>

# آیتم ۳۱: از Bounded Wildcardها برای افزایش انعطاف‌پذیری API استفاده کنید

## (Use bounded wildcards to increase API flexibility)

این Item یکی از مهم‌ترین بخش‌های **Generics در Java** است؛ چون از اینجا به بعد باید بتوانی APIای طراحی کنی که فقط از نظر type-safe بودن درست نباشد، بلکه از نظر **flexibility و substitutability** هم درست طراحی شده باشد.

---

## فهرست مطالب

- [ایده اصلی Item](#core-idea)
- [۱. ابتدا مشکل اصلی: Invariance](#invariance)
- [۲. چرا `push(E)` مشکلی ندارد؟](#push-works)
- [۳. راه‌حل: `? extends E`](#extends-solution)
- [۴. یک نکته بسیار مهم درباره `extends`](#extends-note)
- [۵. چرا نمی‌توانیم داخل `? extends` چیزی اضافه کنیم؟](#why-no-add)
- [۶. حالا `popAll`](#popall)
- [۷. اینجا `? super E`](#super-solution)
- [۸. `? super` را چگونه بفهمیم؟](#understanding-super)
- [۹. تفاوت `extends` و `super`](#extends-vs-super)
- [۱۰. یک مثال بسیار ساده برای PECS](#pecs-example)
- [۱۱. چرا `Collection<T>` گاهی API بدی است؟](#collection-issue)
- [۱۲. مثال `Chooser`](#chooser-example)
- [۱۳. مثال بسیار مهم `union`](#union-example)
- [۱۴. چرا return type نباید wildcard باشد؟](#return-type)
- [۱۵. بخش سخت‌تر: `max`](#max-example)
- [۱۶. قسمت اول `List<? extends T>`](#list-extends)
- [۱۷. قسمت دوم `Comparable<? super T>`](#comparable-super)
- [۱۸. یک مثال واقعی: `ScheduledFuture`](#scheduled-future)
- [۱۹. نکته بسیار مهم درباره Comparable و Comparator](#comparable-comparator)
- [۲۰. یک قانون مهم‌تر از PECS](#pecs-rule)
- [۲۱. Type Parameter vs Wildcard](#type-parameter-vs-wildcard)
- [۲۲. اما چرا `swap(List<?>)` مستقیماً قابل پیاده‌سازی نیست؟](#swap-issue)
- [۲۳. `List<?>` واقعاً چیست؟](#list-wildcard)
- [۲۴. پس چرا Helper Method مشکل را حل می‌کند؟](#helper-method)
- [۲۵. Capture Conversion را ذهنی این‌طور ببین](#capture-conversion)
- [۲۶. Anti-Pattern مهم](#anti-pattern1)
- [۲۷. Anti-Pattern مهم‌تر](#anti-pattern2)
- [۲۸. مدل ذهنی بسیار مهم](#mental-model)
- [۲۹. Producer و Consumer همیشه نسبت به چه چیزی؟](#producer-consumer)
- [۳۰. اگر parameter هم Producer و هم Consumer باشد چه؟](#both)
- [۳۱. جدول تصمیم‌گیری عملی](#decision-table)
- [۳۲. یک مثال Production-Grade](#production-example1)
- [۳۳. یک مثال برای Consumer](#production-example2)
- [۳۴. چرا این موضوع برای Library/API Design بسیار مهم است؟](#api-design-importance)
- [۳۵. ارتباط با Liskov Substitution Principle](#lsp)
- [۳۶. `? extends` و `? super` را با یک مثال واقعی حفظ کن](#real-example)
- [۳۷. یک نکته ظریف: `List<?>` با `List<Object>` یکی نیست](#list-vs-list-object)
- [۳۸. مهم‌ترین تفاوت Generic Type و Wildcard](#generic-vs-wildcard)
- [۳۹. قاعده طلایی برای طراحی API](#golden-rule)
- [۴۰. جمع‌بندی کل Item 31](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ایده اصلی Item

مسئله‌ای که این Item حل می‌کند این است:

> **Generic types در Java به‌صورت invariant هستند، اما در APIها اغلب به covariance یا contravariance نیاز داریم.**

مثلاً:

<div dir="ltr">

```java
List<Integer>
```
</div>

زیرنوع این نیست:

<div dir="ltr">

```java
List<Number>
```
</div>

حتی اگر:

<div dir="ltr">

```text
Integer <: Number
```
</div>

باشد.

بنابراین باید با wildcardها به Java بگوییم:

> من دقیقاً `Number` نمی‌خواهم؛ یک نوعی می‌خواهم که زیرنوع Number باشد.

یا:

> من دقیقاً `Number` نمی‌خواهم؛ هر نوعی که Number بتواند داخل آن قرار بگیرد برایم قابل قبول است.

و اینجا دو مفهوم اصلی وارد می‌شوند:

<div dir="ltr">

```java
? extends T
```
</div>

و

<div dir="ltr">

```java
? super T
```
</div>

که با mnemonic معروف زیر به خاطر سپرده می‌شوند:

# PECS
<div dir="ltr">

> **Producer Extends, Consumer Super**
</div>

[بازگشت به بالا](#top)

---

<a id="invariance"></a>
## ۱. ابتدا مشکل اصلی: Invariance

فرض کن:

<div dir="ltr">

```text
Integer <: Number <: Object
```
</div>

ممکن است انتظار داشته باشیم:

<div dir="ltr">

```text
List<Integer> <: List<Number> <: List<Object>
```
</div>

اما Java این را قبول نمی‌کند:

<div dir="ltr">

```java
List<Integer> integers = ...;
List<Number> numbers = integers; // ❌
```
</div>

چرا؟

چون اگر چنین چیزی مجاز بود:

<div dir="ltr">

```java
List<Integer> integers = new ArrayList<>();
List<Number> numbers = integers;
numbers.add(3.14); // Double
```
</div>

آن‌وقت در واقع یک `Double` وارد `List<Integer>` شده بود.

پس Java برای حفظ type safety می‌گوید:

<div dir="ltr">

```text
Integer <: Number
```
</div>

ولی:

<div dir="ltr">

```text
List<Integer> ≠ subtype of List<Number>
```
</div>

[بازگشت به بالا](#top)

---

<a id="push-works"></a>
## ۲. چرا `push(E)` مشکلی ندارد؟

Stack کتاب را در نظر بگیر:

<div dir="ltr">

```java
public class Stack<E> {
    public void push(E e) { ... }
    public E pop() { ... }
}
```
</div>

اگر داشته باشیم:

<div dir="ltr">

```java
Stack<Number> stack = new Stack<>();
Integer integer = 42;
stack.push(integer);
```
</div>

کاملاً درست است.

چرا؟ چون `Integer → Number` و متد انتظار دارد `push(Number)`. Java می‌تواند `Integer` را به `Number` تبدیل کند.

اما حالا مسئله زمانی ایجاد می‌شود که یک collection از Integer داریم:

<div dir="ltr">

```java
Iterable<Integer> integers = ...;
```
</div>

و می‌خواهیم:

<div dir="ltr">

```java
stack.pushAll(integers);
```
</div>

اگر API این باشد:

<div dir="ltr">

```java
public void pushAll(Iterable<E> src)
```
</div>

برای `Stack<Number>` عملاً تبدیل می‌شود به `pushAll(Iterable<Number>)`، اما ما داریم `Iterable<Integer>` می‌دهیم.

و:

<div dir="ltr">

```text
Iterable<Integer> ❌ Iterable<Number>
```
</div>

[بازگشت به بالا](#top)

---

<a id="extends-solution"></a>
## ۳. راه‌حل: `? extends E`

`pushAll` از `src` مقدار **می‌خواند**:

<div dir="ltr">

```text
src ──────► Stack (produces E)
```
</div>

بنابراین `src` یک **Producer** است.

پس:

<div dir="ltr">

```java
Iterable<? extends E>
```
</div>

یعنی: "یک Iterable از E یا هر subtype از E."

بنابراین:

<div dir="ltr">

```java
public void pushAll(Iterable<? extends E> src) {
    for (E e : src) {
        push(e);
    }
}
```
</div>

حالا:

<div dir="ltr">

```java
Stack<Number> numberStack = new Stack<>();
Iterable<Integer> integers = ...;
numberStack.pushAll(integers);
```
</div>

کاملاً معتبر است.

چون:

<div dir="ltr">

```text
Integer extends Number
```
</div>

پس `Iterable<Integer>` با `Iterable<? extends Number>` مطابقت دارد.

همچنین `Iterable<Double>`، `Iterable<Float>` و حتی `Iterable<Number>` هم قابل قبول هستند.

[بازگشت به بالا](#top)

---

<a id="extends-note"></a>
## ۴. یک نکته بسیار مهم درباره `extends`

این:

<div dir="ltr">

```java
Iterable<? extends Number>
```
</div>

به این معنی نیست که Iterable حتماً از Number ارث‌بری کرده است.

بلکه می‌گوید:

> نوع واقعی داخل Iterable یک subtype از Number است.

ممکن است:

<div dir="ltr">

```java
Iterable<Integer>
Iterable<Double>
Iterable<Float>
Iterable<Number>
```
</div>

باشد.

پس `?` یک **unknown type** است.

[بازگشت به بالا](#top)

---

<a id="why-no-add"></a>
## ۵. چرا نمی‌توانیم داخل `? extends` چیزی اضافه کنیم؟

این نکته برای فهم PECS بسیار مهم است.

فرض کن:

<div dir="ltr">

```java
List<? extends Number> numbers = ...;
```
</div>

می‌توانی از آن بخوانی:

<div dir="ltr">

```java
Number n = numbers.get(0); // ✅
```
</div>

اما نمی‌توانی بگویی:

<div dir="ltr">

```java
numbers.add(42); // ❌
```
</div>

چرا؟ چون Java نمی‌داند `?` دقیقاً چیست. ممکن است `List<Integer>` باشد، ممکن است `List<Double>` باشد.

اگر `numbers.add(42)` مجاز بود و list واقعاً `List<Double>` بود، type safety خراب می‌شد.

بنابراین:

<div dir="ltr">

```text
? extends Number → READ ✅ WRITE ❌
```
</div>

[بازگشت به بالا](#top)

---

<a id="popall"></a>
## ۶. حالا `popAll`

حالا مسئله برعکس است.

Stack:

<div dir="ltr">

```java
Stack<Number>
```
</div>

داده تولید می‌کند.

می‌خواهیم داده‌ها را داخل collection بریزیم:

<div dir="ltr">

```java
Collection<Object>
```
</div>

این باید منطقی باشد:

<div dir="ltr">

```text
Number → Object
```
</div>

مثلاً:

<div dir="ltr">

```java
Object obj = numberStack.pop();
```
</div>

کاملاً درست است.

اما این:

<div dir="ltr">

```java
public void popAll(Collection<E> dst)
```
</div>

برای `Stack<Number>` تبدیل می‌شود به `Collection<Number>`، و بنابراین `Collection<Object>` پذیرفته نمی‌شود.

[بازگشت به بالا](#top)

---

<a id="super-solution"></a>
## ۷. اینجا `? super E`

`dst` داده‌های Stack را **مصرف می‌کند**:

<div dir="ltr">

```text
Stack ──────► dst (consumes E)
```
</div>

پس `dst` یک **Consumer** است.

بنابراین:

<div dir="ltr">

```java
public void popAll(Collection<? super E> dst) {
    while (!isEmpty()) {
        dst.add(pop());
    }
}
```
</div>

حالا برای `Stack<Number>`، این‌ها همگی معتبرند:

<div dir="ltr">

```java
Collection<Number>
Collection<Object>
Collection<? super Number>
```
</div>

چون:

<div dir="ltr">

```text
Number → Object
```
</div>

و Object می‌تواند Number را نگه دارد.

[بازگشت به بالا](#top)

---

<a id="understanding-super"></a>
## ۸. `? super` را چگونه بفهمیم؟

فرض کن:

<div dir="ltr">

```java
Collection<? super Number> collection
```
</div>

یعنی: "Collectionای از Number یا هر supertype از Number."

بنابراین:

<div dir="ltr">

```java
Collection<Number>
Collection<Object>
```
</div>

مجازند.

ولی `Collection<Integer>` مجاز نیست، چون `Integer` supertype از Number نیست.

[بازگشت به بالا](#top)

---

<a id="extends-vs-super"></a>
## ۹. تفاوت `extends` و `super`

|           | `? extends T`            | `? super T`            |
| --------- | ------------------------ | ---------------------- |
| مفهوم     | subtype ناشناخته         | supertype ناشناخته     |
| Producer  | ✅                        | ❌                      |
| Consumer  | ❌                        | ✅                      |
| Read as T | ✅                        | ✅                      |
| Add T     | ❌                        | ✅                      |
| مثال      | `List<? extends Number>` | `List<? super Number>` |

اما قسمت مهم‌تر:

<div dir="ltr">

```text
Producer → extends
Consumer → super
```
</div>

یا:

# PECS

[بازگشت به بالا](#top)

---

<a id="pecs-example"></a>
## ۱۰. یک مثال بسیار ساده برای PECS

فرض کن:

<div dir="ltr">

```java
public void copy(
        List<? extends Number> source,
        List<? super Number> destination) {

    for (Number number : source) {
        destination.add(number);
    }
}
```
</div>

اینجا:

<div dir="ltr">

```text
source → produces Number → Number → consumes Number → destination
```
</div>

پس:

<div dir="ltr">

```java
List<? extends Number> source
List<? super Number> destination
```
</div>

کاملاً طبیعی است.

[بازگشت به بالا](#top)

---

<a id="collection-issue"></a>
## ۱۱. چرا `Collection<T>` گاهی API بدی است؟

اگر بنویسی:

<div dir="ltr">

```java
public void process(Collection<Number> numbers)
```
</div>

فقط `Collection<Number>` را قبول می‌کنی.

ولی اگر متد فقط داده‌ها را می‌خواند:

<div dir="ltr">

```java
public void process(Collection<? extends Number> numbers)
```
</div>

حالا:

<div dir="ltr">

```java
Collection<Integer>
Collection<Double>
Collection<Float>
Collection<Number>
```
</div>

همگی قابل قبول‌اند.

بنابراین wildcard باعث می‌شود API:

<div dir="ltr">

```text
less restrictive → more flexible → more reusable
```
</div>

شود.

[بازگشت به بالا](#top)

---

<a id="chooser-example"></a>
## ۱۲. مثال `Chooser`

در Item 28 احتمالاً داشتیم:

<div dir="ltr">

```java
public Chooser(Collection<T> choices)
```
</div>

این declaration بیش از حد restrictive است.

فرض کن:

<div dir="ltr">

```java
Chooser<Number> chooser =
        new Chooser<>(List.of(1, 2, 3));
```
</div>

`List.of(1,2,3)` از نوع `List<Integer>` است، اما `List<Integer>` با `Collection<Number>` مطابقت ندارد.

بنابراین API بهتر:

<div dir="ltr">

```java
public Chooser(Collection<? extends T> choices)
```
</div>

حالا:

<div dir="ltr">

```java
Chooser<Number> chooser =
        new Chooser<>(List.of(1, 2, 3));
```
</div>

کار می‌کند.

[بازگشت به بالا](#top)

---

<a id="union-example"></a>
## ۱۳. مثال بسیار مهم `union`

فرض کن:

<div dir="ltr">

```java
public static <E> Set<E> union(Set<E> s1, Set<E> s2) { ... }
```
</div>

این API مشکل دارد.

مثلاً:

<div dir="ltr">

```java
Set<Integer> integers = Set.of(1, 3, 5);
Set<Double> doubles = Set.of(2.0, 4.0, 6.0);
```
</div>

می‌خواهیم:

<div dir="ltr">

```java
Set<Number> numbers = union(integers, doubles);
```
</div>

اما:

<div dir="ltr">

```text
Set<Integer> ≠ Set<Number>
Set<Double>  ≠ Set<Number>
```
</div>

پس declaration را تغییر می‌دهیم:

<div dir="ltr">

```java
public static <E> Set<E> union(
        Set<? extends E> s1,
        Set<? extends E> s2) { ... }
```
</div>

حالا:

<div dir="ltr">

```java
Set<Number> numbers = union(integers, doubles);
```
</div>

کار می‌کند.

[بازگشت به بالا](#top)

---

<a id="return-type"></a>
## ۱۴. چرا return type نباید wildcard باشد؟

نویسنده یک نکته بسیار مهم API Design می‌گوید:
<div dir="ltr">

> **Do not use bounded wildcard types as return types.**
</div>
مثلاً این:

<div dir="ltr">

```java
public Set<? extends Number> getNumbers()
```
</div>

معمولاً API خوبی نیست.

چرا؟ چون client حالا باید با wildcard کار کند:

<div dir="ltr">

```java
Set<? extends Number> numbers = getNumbers();
```
</div>

در حالی که اگر واقعاً نوع مشخصی داریم بهتر است:

<div dir="ltr">

```java
public Set<Number> getNumbers()
```
</div>

یا generic type مناسب داشته باشیم.

قاعده عملی:

<div dir="ltr">

```text
Input parameter → wildcard often useful
Return type → usually avoid wildcard
```
</div>

[بازگشت به بالا](#top)

---

<a id="max-example"></a>
## ۱۵. بخش سخت‌تر: `max`

نسخه اولیه:

<div dir="ltr">

```java
public static <T extends Comparable<T>> T max(List<T> list)
```
</div>

نسخه اصلاح‌شده:

<div dir="ltr">

```java
public static <T extends Comparable<? super T>> T max(
        List<? extends T> list)
```
</div>

بیایید آن را تکه‌تکه کنیم.

[بازگشت به بالا](#top)

---

<a id="list-extends"></a>
## ۱۶. قسمت اول `<List<? extends T`

متد `max` از list مقدار می‌خواند:

<div dir="ltr">

```java
T value = list.get(0);
```
</div>

پس `List → Producer`.

بنابراین `List<T>` تبدیل می‌شود به `List<? extends T>` تا list بتواند subtypeهای T را هم داشته باشد.

[بازگشت به بالا](#top)

---

<a id="comparable-super"></a>
## ۱۷. قسمت دوم `Comparable<? super T>`

نسخه ساده:

<div dir="ltr">

```java
T extends Comparable<T>
```
</div>

یعنی: "T باید Comparable خودش باشد."

مثلاً:

<div dir="ltr">

```java
class Person implements Comparable<Person>
```
</div>

اما همیشه این‌طور نیست.

ممکن است:

<div dir="ltr">

```java
class MyType implements Comparable<BaseType>
```
</div>

باشد. اگر `MyType <: BaseType` است، آیا MyType قابل مقایسه با BaseType است؟ بله.

پس declaration زیر بیش از حد restrictive است:

<div dir="ltr">

```java
T extends Comparable<T>
```
</div>

چون فقط می‌گوید `Comparable<T>`، اما باید اجازه بدهیم `Comparable<T>` یا `Comparable<supertype-of-T>`.

پس:

<div dir="ltr">

```java
Comparable<? super T>
```
</div>

[بازگشت به بالا](#top)

---

<a id="scheduled-future"></a>
## ۱۸. یک مثال واقعی: `ScheduledFuture`

نویسنده مثال بسیار خوبی دارد:

<div dir="ltr">

```java
List<ScheduledFuture<?>> scheduledFutures = ...;
```
</div>

`ScheduledFuture` مستقیماً این را implement نمی‌کند:

<div dir="ltr">

```java
Comparable<ScheduledFuture>
```
</div>

بلکه chain به این شکل است:

<div dir="ltr">

```text
ScheduledFuture → Delayed → Comparable<Delayed>
```
</div>

پس `ScheduledFuture <: Delayed` و `Delayed implements Comparable<Delayed>`.

اما declaration قدیمی:

<div dir="ltr">

```java
<T extends Comparable<T>>
```
</div>

می‌گوید: "ScheduledFuture must implement Comparable<ScheduledFuture>" که درست نیست.

اما:

<div dir="ltr">

```java
<T extends Comparable<? super T>>
```
</div>

می‌گوید: "T باید با خودش یا یکی از supertypes خودش قابل مقایسه باشد." و این دقیقاً چیزی است که لازم داریم.

[بازگشت به بالا](#top)

---

<a id="comparable-comparator"></a>
## ۱۹. نکته بسیار مهم درباره Comparable و Comparator

`Comparable<T>` را ببین:

<div dir="ltr">

```java
public interface Comparable<T> {
    int compareTo(T o);
}
```
</div>

این interface یک `T` **دریافت می‌کند**. پس `T` در اینجا Consumer است.

بنابراین:

<div dir="ltr">

```java
Comparable<? super T>
```
</div>

معمولاً بهتر از:

<div dir="ltr">

```java
Comparable<T>
```
</div>

است.

همین اصل برای `Comparator<T>` هم صدق می‌کند:

<div dir="ltr">

```java
Comparator<? super T>
```
</div>

معمولاً API flexibleتری است.

[بازگشت به بالا](#top)

---

<a id="pecs-rule"></a>
## ۲۰. یک قانون مهم‌تر از PECS

<div dir="ltr">

```java
Comparable<? super T>
```
</div>

و:

<div dir="ltr">

```java
Comparator<? super T>
```
</div>

تقریباً یک idiom استاندارد Java API هستند.

مثلاً در APIهای استاندارد Java زیاد می‌بینی:

<div dir="ltr">

```java
Comparator<? super T>
```
</div>

چون Comparator چیزی را **مصرف** می‌کند که با آن مقایسه انجام می‌دهد.

[بازگشت به بالا](#top)

---

<a id="type-parameter-vs-wildcard"></a>
## ۲۱. Type Parameter vs Wildcard

این دو:

<div dir="ltr">

```java
public static <E> void swap(List<E> list, int i, int j)
```
</div>

و:

<div dir="ltr">

```java
public static void swap(List<?> list, int i, int j)
```
</div>

از نظر caller تقریباً یک مفهوم دارند.

اما API دوم بهتر است:

<div dir="ltr">

```java
public static void swap(List<?> list, int i, int j)
```
</div>

چرا؟ چون `E` فقط یک بار در declaration ظاهر شده است.

قاعده:

> **اگر type parameter فقط یک بار در method declaration ظاهر می‌شود، معمولاً آن را با wildcard جایگزین کن.**

[بازگشت به بالا](#top)

---

<a id="swap-issue"></a>
## ۲۲. اما چرا `swap(List<?>)` مستقیماً قابل پیاده‌سازی نیست؟

این کد:

<div dir="ltr">

```java
public static void swap(List<?> list, int i, int j) {
    list.set(i, list.set(j, list.get(i)));
}
```
</div>

compile نمی‌شود.

دلیلش مفهوم بسیار مهمی به نام **Capture of Wildcard** است.

[بازگشت به بالا](#top)

---

<a id="list-wildcard"></a>
## ۲۳. `List<?>` واقعاً چیست؟

فرض کن:

<div dir="ltr">

```java
List<?> list
```
</div>

نوع واقعی می‌تواند باشد:

<div dir="ltr">

```java
List<String>
List<Integer>
List<Customer>
List<Order>
```
</div>

Java می‌گوید:

<div dir="ltr">

```text
? = یک نوع ناشناخته است
```
</div>

در ذهن compiler:

<div dir="ltr">

```text
List<CAP#1> که CAP#1 = unknown type است
```
</div>

تو می‌توانی از list بخوانی:

<div dir="ltr">

```java
Object value = list.get(0);
```
</div>

چون هر چیزی در نهایت Object است.

ولی نمی‌توانی بگویی:

<div dir="ltr">

```java
list.add("hello");
```
</div>

چون شاید `List<Integer>` باشد.

[بازگشت به بالا](#top)

---

<a id="helper-method"></a>
## ۲۴. پس چرا Helper Method مشکل را حل می‌کند؟

نویسنده می‌گوید wildcard را capture کنیم:

<div dir="ltr">

```java
public static void swap(List<?> list, int i, int j) {
    swapHelper(list, i, j);
}
```
</div>

و:

<div dir="ltr">

```java
private static <E> void swapHelper(
        List<E> list, int i, int j) {

    list.set(i, list.set(j, list.get(i)));
}
```
</div>

اینجا compiler می‌تواند بگوید:

<div dir="ltr">

```text
? → E
```
</div>

و حالا `List<E>` داریم.

پس:

<div dir="ltr">

```java
E value = list.get(i);
```
</div>

و:

<div dir="ltr">

```java
list.set(j, value);
```
</div>

امن است. چون همان نوع ناشناخته `E` است.

[بازگشت به بالا](#top)

---

<a id="capture-conversion"></a>
## ۲۵. Capture Conversion را ذهنی این‌طور ببین

وقتی داری:

<div dir="ltr">

```java
List<?> list
```
</div>

compiler می‌گوید:

<div dir="ltr">

```text
List<CAP>
```
</div>

و:

<div dir="ltr">

```text
CAP = unknown but fixed type
```
</div>

Helper method می‌گوید:

<div dir="ltr">

```java
<E>
```
</div>

و compiler آن را با همان CAP match می‌کند:

<div dir="ltr">

```text
E = CAP
```
</div>

پس:

<div dir="ltr">

```text
List<CAP> → List<E>
```
</div>

بدون هیچ cast ناامنی.

[بازگشت به بالا](#top)

---

<a id="anti-pattern1"></a>
## ۲۶. Anti-Pattern مهم

یکی از اشتباهات رایج:

<div dir="ltr">

```java
public void process(List<Object> items)
```
</div>

وقتی واقعاً می‌خواهی هر نوع objectای را قبول کنی.

این `List<String>` را قبول نمی‌کند.

اگر فقط می‌خواهی list را بخوانی:

<div dir="ltr">

```java
public void process(List<?> items)
```
</div>

بهتر است.

[بازگشت به بالا](#top)

---

<a id="anti-pattern2"></a>
## ۲۷. Anti-Pattern مهم‌تر: استفاده افراطی از wildcard

نباید تصور کنیم:

> هر جا generic دیدم باید wildcard اضافه کنم.

Wildcard زمانی ارزش دارد که **flexibility مورد نیاز API** را افزایش دهد.

[بازگشت به بالا](#top)

---

<a id="mental-model"></a>
## ۲۸. مدل ذهنی بسیار مهم

به جای حفظ کردن syntax، این مدل را داشته باش:

<div dir="ltr">

```text
                 ┌──────────────┐
                 │ Generic API  │
                 └──────┬───────┘
                        │
              What does parameter do?
                        │
             ┌──────────┴──────────┐
             │                     │
          Produces              Consumes
             │                     │
             ▼                     ▼
      ? extends T              ? super T
             │                     │
             ▼                     ▼
           PECS                  PECS
```
</div>

مثلاً:

<div dir="ltr">

```java
void pushAll(Iterable<? extends E> src)
```
</div>

چون: `src → produces E`

و:

<div dir="ltr">

```java
void popAll(Collection<? super E> dst)
```
</div>

چون: `dst → consumes E`

[بازگشت به بالا](#top)

---

<a id="producer-consumer"></a>
## ۲۹. Producer و Consumer همیشه نسبت به چه چیزی؟

این نکته ظریف است.

نباید بگویی: "این Collection producer است."

باید بگویی: "این Collection نسبت به **T** producer است."

مثلاً:

<div dir="ltr">

```java
List<? extends Number>
```
</div>

producer `Number` است.

ولی:

<div dir="ltr">

```java
List<? super Number>
```
</div>

consumer `Number` است.

Producer/Consumer بودن **نقش parameter در API** است، نه یک ویژگی ذاتی class.

[بازگشت به بالا](#top)

---

<a id="both"></a>
## ۳۰. اگر parameter هم Producer و هم Consumer باشد چه؟

این قسمت را نویسنده خیلی مهم می‌داند.

مثلاً:

<div dir="ltr">

```java
void process(List<E> list) {
    E e = list.get(0);  // producer
    list.add(e);        // consumer
}
```
</div>

اینجا list هم Producer است و هم Consumer.

در چنین شرایطی wildcard معمولاً کمک نمی‌کند. باید نوع دقیق مشخص باشد: `List<E>`

[بازگشت به بالا](#top)

---

<a id="decision-table"></a>
## ۳۱. جدول تصمیم‌گیری عملی

| وضعیت parameter | انتخاب |
|-----------------|--------|
| فقط T تولید می‌کند | `? extends T` |
| فقط T مصرف می‌کند | `? super T` |
| هم تولید و هم مصرف می‌کند | `T` |
| نوع فقط یک بار ظاهر شده | معمولاً `?` |
| Comparable | `Comparable<? super T>` |
| Comparator | `Comparator<? super T>` |
| Return type | معمولاً wildcard نگذار |

[بازگشت به بالا](#top)

---

<a id="production-example1"></a>
## ۳۲. یک مثال Production-Grade

فرض کن یک سرویس batch processing داریم:

<div dir="ltr">

```java
public interface BatchProcessor<T> {
    void process(Iterable<? extends T> items);
}
```
</div>

چرا `extends`؟ چون processor از collection داده دریافت می‌کند:

<div dir="ltr">

```text
Iterable → produces T → Processor
```
</div>

حالا:

<div dir="ltr">

```java
BatchProcessor<Number> processor = ...;
List<Integer> integers = ...;
processor.process(integers);
```
</div>

کاملاً معتبر است. اگر می‌نوشتیم `void process(Iterable<T> items)` این flexibility را از دست می‌دادیم.

[بازگشت به بالا](#top)

---

<a id="production-example2"></a>
## ۳۳. یک مثال برای Consumer

فرض کن exporter داریم:

<div dir="ltr">

```java
public interface Exporter<T> {
    void export(
        Iterable<? extends T> source,
        Collection<? super T> destination
    );
}
```
</div>

اینجا:

<div dir="ltr">

```text
source → produces T
destination ← consumes T
```
</div>

بنابراین:

<div dir="ltr">

```java
Iterable<? extends T>
Collection<? super T>
```
</div>

کاملاً طبیعی است.

[بازگشت به بالا](#top)

---

<a id="api-design-importance"></a>
## ۳۴. چرا این موضوع برای Library/API Design بسیار مهم است؟

فرض کن API عمومی تو این باشد:

<div dir="ltr">

```java
public void addAll(Collection<T> items)
```
</div>

و بعد library منتشر شود.

کاربر می‌گوید:

<div dir="ltr">

```java
List<SubType> values = ...;
api.addAll(values);
```
</div>

و API تو رد می‌کند. در حالی که از لحاظ منطقی باید بتواند.

اگر از ابتدا نوشته بودی:

<div dir="ltr">

```java
public void addAll(Collection<? extends T> items)
```
</div>

API انعطاف‌پذیرتر بود.

این یعنی wildcard فقط یک syntax trick نیست. در واقع بخشی از **API Contract** است.

[بازگشت به بالا](#top)

---

<a id="lsp"></a>
## ۳۵. ارتباط با Liskov Substitution Principle

ممکن است:

<div dir="ltr">

```text
Integer <: Number
```
</div>

اما:

<div dir="ltr">

```text
List<Integer> <: List<Number>
```
</div>

نباشد.

چون `List` mutable است و هم producer است و هم consumer.

اگر List covariance داشت، type safety از بین می‌رفت.

Wildcard به ما اجازه می‌دهد **variance را در محل استفاده** بیان کنیم.

مثلاً:

<div dir="ltr">

```java
List<? extends Number>
```
</div>

یعنی: "من فقط به aspect producer این List اهمیت می‌دهم."

و:

<div dir="ltr">

```java
List<? super Number>
```
</div>

یعنی: "من فقط به aspect consumer آن اهمیت می‌دهم."

[بازگشت به بالا](#top)

---

<a id="real-example"></a>
## ۳۶. `? extends` و `? super` را با یک مثال واقعی حفظ کن

فرض کن:

<div dir="ltr">

```text
Number
├── Integer
├── Double
└── Long
```
</div>

### Producer

<div dir="ltr">

```java
List<? extends Number>
```
</div>

می‌تواند: `List<Integer>`، `List<Double>`، `List<Long>`، `List<Number>` باشد.

تو از آن می‌خوانی:

<div dir="ltr">

```java
Number n = list.get(0);
```
</div>

### Consumer

<div dir="ltr">

```java
List<? super Number>
```
</div>

می‌تواند: `List<Number>`، `List<Object>` باشد.

تو داخل آن می‌نویسی:

<div dir="ltr">

```java
list.add(42);
list.add(3.14);
```
</div>

چون هر دو `Number` هستند.

[بازگشت به بالا](#top)

---

<a id="list-vs-list-object"></a>
## ۳۷. یک نکته ظریف: `List<?>` با `List<Object>` یکی نیست

### `List<Object>`

یعنی: "List دقیقاً از Object تشکیل شده است."

<div dir="ltr">

```java
List<Object> list = new ArrayList<>();
list.add("hello");
list.add(42);
```
</div>

### `List<?>`

یعنی: "List از یک نوع ناشناخته تشکیل شده است."

مثلاً `List<String>`، `List<Integer>` یا `List<Customer>`.

پس:

<div dir="ltr">

```java
List<?> list = List.of("A", "B");
```
</div>

مجاز است. اما:

<div dir="ltr">

```java
list.add("C"); // ❌
```
</div>

[بازگشت به بالا](#top)

---

<a id="generic-vs-wildcard"></a>
## ۳۸. مهم‌ترین تفاوت Generic Type و Wildcard

### Type Parameter

<div dir="ltr">

```java
<T>
```
</div>

وقتی می‌خواهی **یک نوع را بین چند بخش declaration مرتبط کنی**.

مثلاً:

<div dir="ltr">

```java
public <T> T identity(T value)
```
</div>

`T` در input و output ارتباط ایجاد می‌کند.

### Wildcard

<div dir="ltr">

```java
?
```
</div>

وقتی فقط می‌خواهی **محدوده قابل قبول یک type را مشخص کنی**.

مثلاً:

<div dir="ltr">

```java
void print(List<?> list)
```
</div>

اینجا هیچ ارتباطی بین type parameterهای مختلف وجود ندارد.

[بازگشت به بالا](#top)

---

<a id="golden-rule"></a>
## ۳۹. قاعده طلایی برای طراحی API

وقتی method می‌نویسی، این سؤال‌ها را به ترتیب بپرس:

<div dir="ltr">

```text
1. Parameter چه چیزی است؟
          ↓
2. آیا T تولید می‌کند؟ → extends
          ↓
3. آیا T مصرف می‌کند؟ → super
          ↓
4. آیا هم تولید و هم مصرف می‌کند؟ → exact T
```
</div>

و سپس:

<div dir="ltr">

```text
5. آیا type parameter فقط یک بار آمده؟ → wildcard
```
</div>

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۴۰. جمع‌بندی کل Item 31

اگر بخواهم کل Item را در چند خط خلاصه کنم:

<div dir="ltr">

```java
Producer → ? extends T
Consumer → ? super T
Both     → T
```
</div>

مثال اصلی کتاب:

<div dir="ltr">

```java
public void pushAll(Iterable<? extends E> src)   // src produces E
public void popAll(Collection<? super E> dst)    // dst consumes E
```
</div>

برای Comparable:

<div dir="ltr">

```java
Comparable<? super T>
```
</div>

و برای Comparator:

<div dir="ltr">

```java
Comparator<? super T>
```
</div>

و برای public API:

<div dir="ltr">

```java
public static void swap(List<?> list, ...)
```
</div>

معمولاً از:

<div dir="ltr">

```java
public static <E> void swap(List<E> list, ...)
```
</div>

بهتر است، اگر `E` فقط یک بار در declaration مورد نیاز باشد.

---

### نکته‌ای که برای سطح Senior/Architect باید از این Item بگیری

Item 31 در ظاهر درباره syntax مربوط به `? extends` و `? super` است، اما در سطح معماری‌تر، درباره این اصل است:

> **API باید حداقل محدودیت type را اعمال کند که برای حفظ type-safety لازم است.**

یعنی API را بی‌دلیل این‌طور محدود نکن:

<div dir="ltr">

```java
Collection<T>
```
</div>

اگر واقعاً فقط producer است:

<div dir="ltr">

```java
Collection<? extends T>
```
</div>

و اگر فقط consumer است:

<div dir="ltr">

```java
Collection<? super T>
```
</div>

این موضوع مستقیماً روی **reusability، substitutability و evolvability یک API عمومی** اثر می‌گذارد.

و مهم‌تر از همه:

> **PECS را صرفاً به‌عنوان یک mnemonic حفظ نکن؛ جریان داده را تشخیص بده.**

اگر داده از parameter **به سمت method** حرکت می‌کند:

<div dir="ltr">

```text
parameter → method → Producer → extends
```
</div>

اگر داده از method **به سمت parameter** حرکت می‌کند:

<div dir="ltr">

```text
method → parameter → Consumer → super
```
</div>

این دقیقاً همان چیزی است که باعث می‌شود `pushAll` و `popAll` در کتاب تفاوت داشته باشند.

---

[بازگشت به بالا](#top)

</div>