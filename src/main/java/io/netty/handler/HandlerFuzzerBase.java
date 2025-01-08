// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package io.netty.handler;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import io.micronaut.fuzzing.Dict;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Base class for fuzzing the input of an inbound handler. Will report exceptions thrown by the handler.
 */
public abstract class HandlerFuzzerBase {
    protected final EmbeddedChannel channel = new EmbeddedChannel();

    public void test(FuzzedDataProvider provider) {
        ByteBuf joint = channel.alloc().buffer(provider.remainingBytes());
        joint.writeBytes(provider.consumeRemainingAsBytes());
        ByteSeparator.forEachPiece(joint, msg -> {
            if (channel.isOpen()) {
                channel.writeInbound(msg);
            } else {
                msg.release();
            }
        });
        channel.finishAndReleaseAll();
        channel.checkException();
    }
}
