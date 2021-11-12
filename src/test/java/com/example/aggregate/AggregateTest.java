package com.example.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.Test;

public class AggregateTest {
  @Test
  public void test() {
    final int[] buckets = new int[1000];
    final int[] res = new int[1000];

    Arrays.parallelSetAll(buckets, i -> i);
    Arrays.parallelPrefix(buckets, (x,y) -> x+y);

    Arrays.parallelSetAll(res, idx -> buckets[idx] + (idx>1? buckets[idx-2]*-1 : 0));

    assertThat(res).doesNotHaveDuplicates();
    assertThat(res).startsWith(0,1,3,5);
    assertThat(res).endsWith(1991,1993,1995,1997);
  }

  @Test
  public void anotherTest() {
    final int[] arr = new int[1000];
    final int[] arr2 = new int[1000];

    Arrays.parallelSetAll(arr, i -> ((3 + 0 - i%3)%3));
    Arrays.parallelSetAll(arr2, i -> 99-i);
    Arrays.parallelSetAll(arr2, i -> arr2[i]%3);
  }
}
