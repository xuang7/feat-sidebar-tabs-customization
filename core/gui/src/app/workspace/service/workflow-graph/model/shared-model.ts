/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import * as Y from "yjs";
import { WebsocketProvider } from "y-websocket";
import { Awareness } from "y-protocols/awareness";
import {
  CommentBox,
  OperatorLink,
  OperatorPredicate,
  Point,
  BreakpointInfo,
} from "../../../types/workflow-common.interface";
import { CoeditorState, User } from "../../../../common/type/user";
import { getWebsocketUrl } from "../../../../common/util/url";
import { v4 as uuid } from "uuid";
import { YType } from "../../../types/shared-editing.interface";

/**
 * SharedModel encapsulates everything related to real-time shared editing for the current workflow.
 * Most of the yjs-related implementations are within this class.
 */
export class SharedModel {
  public yDoc: Y.Doc = new Y.Doc();
  public wsProvider: WebsocketProvider;
  public awareness: Awareness;
  public operatorIDMap: Y.Map<YType<OperatorPredicate>>;
  public commentBoxMap: Y.Map<YType<CommentBox>>;
  public operatorLinkMap: Y.Map<OperatorLink>;
  public elementPositionMap: Y.Map<Point>;
  public debugState: Y.Map<Y.Map<BreakpointInfo>>;
  public undoManager: Y.UndoManager;
  public clientId: string;

  /**
   * Initializes yjs-related structures and join the shared-editing room. A room number is required for initialization.
   * When wid is present, it will be used as part of the room number to enable shared editing.
   * When no wid is provided (new workflow canvas, landing page, etc.), a random room number will be assigned so that
   * users don't interfere with each other.
   * @param wid workflow ID number, used as part of the address for the shared-editing room.
   * @param user current (local) user info, used for initializing local awareness (user presence).
   * @param productionSharedEditingServer whether to use production shared editing server
   */
  constructor(
    public wid?: number,
    public user?: User,
    private productionSharedEditingServer?: boolean
  ) {
    // Initialize Y-structures.
    this.debugState = this.yDoc.getMap("debugActions");
    this.operatorIDMap = this.yDoc.getMap("operatorIDMap");
    this.commentBoxMap = this.yDoc.getMap("commentBoxMap");
    this.operatorLinkMap = this.yDoc.getMap("operatorLinkMap");
    this.elementPositionMap = this.yDoc.getMap("elementPositionMap");

    // Initialize Y-undo manager by aggregating intended  Y-structures. Only structures included here will be undoable.
    this.undoManager = new Y.UndoManager(
      [this.operatorIDMap, this.elementPositionMap, this.operatorLinkMap, this.commentBoxMap],
      {
        captureTimeout: 100,
      }
    );

    // Generate editing room number.
    const websocketUrl = this.getYWebSocketBaseUrl();
    const suffix = wid ? `${wid}` : uuid();
    this.wsProvider = new WebsocketProvider(websocketUrl, suffix, this.yDoc);

    // Initialize local user awareness information.
    this.awareness = this.wsProvider.awareness;
    this.clientId = this.awareness.clientID.toString();
    if (this.user) {
      const userState: CoeditorState = {
        user: { ...this.user, clientId: this.clientId },
        isActive: true,
        userCursor: { x: 0, y: 0 },
      };
      this.awareness.setLocalState(userState);
    }
  }

  /**
   * Shared editing needs y-websocket to be running. The base url depends on whether reverse proxy is set up. For local
   * development, we need to use localhost; For production server which has reverse proxy, we can use the same base url
   * as the server.
   * @private
   */
  private getYWebSocketBaseUrl() {
    return getWebsocketUrl("rtc", "");
  }

  /**
   * Updates a particular field of local awareness state info. Will only execute update when user info is provided.
   * @param field the name of the particular state info.
   * @param value the updated state info.
   */
  public updateAwareness<K extends keyof CoeditorState>(field: K, value: CoeditorState[K]): void {
    if (this.user) this.awareness.setLocalStateField(field, value);
  }

  /**
   * Groups a bunch of actions into one atomic transaction, so that they can be undone/redone in one call.
   * @param callback Put whatever need to be atomically done within this callback function.
   */
  public transact(callback: Function) {
    this.yDoc.transact(() => callback());
  }

  /**
   * Destroys internal structures related to Yjs and quit the editing room.
   */
  public destroy(): void {
    this.awareness.destroy();
    try {
      if (this.wsProvider.shouldConnect && this.wsProvider.wsconnected) this.wsProvider.disconnect();
    } catch (e) {}
    this.yDoc.destroy();
  }
}
