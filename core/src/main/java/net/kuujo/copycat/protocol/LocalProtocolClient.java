/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat.protocol;

import net.kuujo.copycat.internal.ThreadExecutionContext;
import net.kuujo.copycat.internal.util.concurrent.NamedThreadFactory;
import net.kuujo.copycat.spi.ExecutionContext;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Local protocol client implementation.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class LocalProtocolClient implements ProtocolClient {
  private final ExecutionContext context = new ThreadExecutionContext(new NamedThreadFactory("copycat-protocol-thread-%d"));
  private final String address;
  private final Map<String, LocalProtocolServer> registry;

  LocalProtocolClient(String address, Map<String, LocalProtocolServer> registry) {
    this.address = address;
    this.registry = registry;
  }

  @Override
  public CompletableFuture<ByteBuffer> write(ByteBuffer request) {
    request.rewind();
    CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
    context.execute(() -> {
      LocalProtocolServer server = registry.get(address);
      if (server != null) {
        server.handle(request).whenComplete((result, error) -> {
          if (error != null) {
            context.submit(() -> future.completeExceptionally(error));
          } else {
            context.submit(() -> future.complete(result));
          }
        });
      } else {
        future.completeExceptionally(new ProtocolException(String.format("Invalid server address %s", address)));
      }
    });
    return future;
  }

  @Override
  public CompletableFuture<Void> connect() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> close() {
    return CompletableFuture.completedFuture(null);
  }

}