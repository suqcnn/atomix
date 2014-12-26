/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat.collections.internal.collection;

import net.kuujo.copycat.CopycatState;
import net.kuujo.copycat.StateMachine;
import net.kuujo.copycat.cluster.Cluster;
import net.kuujo.copycat.collections.AsyncCollection;
import net.kuujo.copycat.collections.AsyncCollectionProxy;
import net.kuujo.copycat.internal.util.concurrent.Futures;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Abstract asynchronous collection implementation.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public abstract class AbstractAsyncCollection<T extends CollectionState<T, V>, U extends AsyncCollectionProxy<V>, V> implements AsyncCollection<V> {
  private final StateMachine<T> stateMachine;
  private final Class<U> proxyClass;
  protected U proxy;

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected AbstractAsyncCollection(StateMachine<T> stateMachine, Class proxyClass) {
    this.stateMachine = stateMachine;
    this.proxyClass = proxyClass;
  }

  @Override
  public String name() {
    return stateMachine.name();
  }

  @Override
  public Cluster cluster() {
    return stateMachine.cluster();
  }

  @Override
  public CopycatState state() {
    return stateMachine.state();
  }

  /**
   * If the collection is closed, returning a failed CompletableFuture. Otherwise, calls the given supplier to
   * return the completed future result.
   *
   * @param supplier The supplier to call if the collection is open.
   * @param <T> The future result type.
   * @return A completable future that if this collection is closed is immediately failed.
   */
  protected <T> CompletableFuture<T> checkOpen(Supplier<CompletableFuture<T>> supplier) {
    if (proxy == null) {
      return Futures.exceptionalFuture(new IllegalStateException("Collection closed"));
    }
    return supplier.get();
  }

  @Override
  public CompletableFuture<Boolean> add(V value) {
    return checkOpen(() -> proxy.add(value));
  }

  @Override
  public CompletableFuture<Boolean> addAll(Collection<? extends V> values) {
    return checkOpen(() -> proxy.addAll(values));
  }

  @Override
  public CompletableFuture<Boolean> retainAll(Collection<?> values) {
    return checkOpen(() -> proxy.retainAll(values));
  }

  @Override
  public CompletableFuture<Boolean> remove(Object value) {
    return checkOpen(() -> proxy.remove(value));
  }

  @Override
  public CompletableFuture<Boolean> removeAll(Collection<?> values) {
    return checkOpen(() -> proxy.removeAll(values));
  }

  @Override
  public CompletableFuture<Boolean> contains(Object value) {
    return checkOpen(() -> proxy.contains(value));
  }

  @Override
  public CompletableFuture<Boolean> containsAll(Collection<?> values) {
    return checkOpen(() -> proxy.containsAll(values));
  }

  @Override
  public CompletableFuture<Integer> size() {
    return checkOpen(proxy::size);
  }

  @Override
  public CompletableFuture<Boolean> isEmpty() {
    return checkOpen(proxy::isEmpty);
  }

  @Override
  public CompletableFuture<Void> clear() {
    return checkOpen(proxy::clear);
  }

  @Override
  public CompletableFuture<Void> delete() {
    return stateMachine.delete();
  }

  @Override
  public CompletableFuture<Void> open() {
    return stateMachine.open().thenRun(() -> {
      proxy = stateMachine.createProxy(proxyClass);
    });
  }

  @Override
  public CompletableFuture<Void> close() {
    proxy = null;
    return stateMachine.close();
  }

}