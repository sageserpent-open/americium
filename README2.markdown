# Americium - **_Property based testing for Java and Scala! Automatic test case shrinkage! Bring your own test style._**

[![Maven Central](https://index.scala-lang.org/sageserpent-open/americium/americium/latest-by-scala-version.svg?color=2465cd&style=flat)](https://index.scala-lang.org/sageserpent-open/americium/americium)

## Example

Some code we're not sure about...

```java
public class PoorQualityGrouping {
    // Where has this implementation gone wrong? Surely we've thought of
    // everything?
    public static <Element> List<List<Element>> groupsOfAdjacentDuplicates(
            List<Element> elements) {
        final Iterator<Element> iterator = elements.iterator();

        final List<List<Element>> result = new LinkedList<>();

        final LinkedList<Element> chunk = new LinkedList<>();

        while (iterator.hasNext()) {
            final Element element = iterator.next();

            // Got to clear the chunk when the element changes...
            if (!chunk.isEmpty() && chunk.get(0) != element) {
                // Got to add the chunk to the result before it gets cleared
                // - and watch out for empty chunks...
                if (!chunk.isEmpty()) result.add(chunk);
                chunk.clear();
            }

            // Always add the latest element to the chunk...
            chunk.add(element);
        }

        // Don't forget to add the last chunk to the result - as long as it's
        // not empty...
        if (!chunk.isEmpty()) result.add(chunk);

        return result;
    }
}
```

Let's test it - we'll use the integration with JUnit5 here...

```java
class GroupingTest {
    private static final TrialsScaffolding.SupplyToSyntax<ImmutableList<Integer>>
            testConfiguration = Trials
            .api()
            .integers(1, 10)
            .immutableLists()
            .withLimit(15);

    @ConfiguredTrialsTest("testConfiguration")
    void groupingShouldNotLoseOrGainElements(List<Integer> integerList) {
        final List<List<Integer>> groups =
                PoorQualityGrouping.groupsOfAdjacentDuplicates(integerList);

        final int size =
                groups.stream().map(List::size).reduce(Integer::sum).orElse(0);

        assertThat(size, equalTo(integerList.size()));
    }
}
```

What happens?

- Americium runs the same test repeatedly against different test case inputs, and finds a failing test case. Oh dear...

![](./screenshots/FailingExample.png)

- The first failing test case leads to an automatic shrinkage process that yields a maximally shrunk test case. See how
  the failing test case's values lie between 1 and 10, just as specified in the test. Shrinking respects the constraints
  we configured into our test data...

![](./screenshots/Shrinkage.png)

- Americium also tells us what the maximally shrunk test case was and how to reproduce it immediately when we re-run the
  test...

```
Case:
[1, 1, 2]
Reproduce via Java property:
trials.recipeHash=3b2a3709bf92b8551b2e9ae0b8b6d526
Reproduce via Java property:
trials.recipe="[{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":2}},{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":1}},{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":1}},{\"ChoiceOf\":{\"index\":0}}]"
```

![](./screenshots/Reproduction.png)

Now go and fix it!



