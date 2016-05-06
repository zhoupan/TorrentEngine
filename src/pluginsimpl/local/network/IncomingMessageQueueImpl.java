/*
 * Created on Feb 9, 2005
 * Created by Alon Rohter
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package pluginsimpl.local.network;

import plugins.messaging.Message;
import plugins.network.IncomingMessageQueue;
import plugins.network.IncomingMessageQueueListener;
import java.io.IOException;
import java.util.HashMap;

import plugins.messaging.bittorrent.BTMessageManager;
import pluginsimpl.local.messaging.MessageAdapter;

import connect.peermanager.messaging.bittorrent.BTMessage;



/**
 *
 */
public class IncomingMessageQueueImpl implements IncomingMessageQueue {
  private final controller.networkmanager.IncomingMessageQueue core_queue;
  private final HashMap registrations = new HashMap();
  
  
  protected IncomingMessageQueueImpl( controller.networkmanager.IncomingMessageQueue core_queue ) {
    this.core_queue = core_queue;
  } 

  public void registerListener( IncomingMessageQueueListener listener ) {
	  registerListenerSupport( listener, false );
  }
  
  public void registerPriorityListener( IncomingMessageQueueListener listener ) {
	  registerListenerSupport( listener, true );
  }
  
  private void registerListenerSupport( final IncomingMessageQueueListener listener, final boolean is_priority ) {
    controller.networkmanager.IncomingMessageQueue.MessageQueueListener core_listener = 
      new controller.networkmanager.IncomingMessageQueue.MessageQueueListener() {
        public boolean messageReceived( connect.peer.messaging.Message message ) {
          if( message instanceof MessageAdapter ) {
            //the message must have been originally decoded by plugin decoder
            //so just use original plugin message...i.e. unwrap out of MessageAdapter
            return listener.messageReceived( ((MessageAdapter)message).getPluginMessage() );
          }
          
          //message originally decoded by core
          
          if ( message instanceof BTMessage ){
        	  
              return listener.messageReceived( BTMessageManager.wrapCoreMessage((BTMessage)message ));
              
          }else{
          
        	  return listener.messageReceived( new MessageAdapter( message ));
          }
        }
      
        public void protocolBytesReceived( int byte_count ) {  listener.bytesReceived( byte_count );  }

        public void dataBytesReceived( int byte_count ) {  listener.bytesReceived( byte_count );  }
        
        public boolean 
        isPriority() 
        {
        	return( is_priority );
        }
    };
    
    registrations.put( listener, core_listener );  //save this mapping for later
    
    core_queue.registerQueueListener( core_listener );
  }
    

  public void deregisterListener( IncomingMessageQueueListener listener ) {
    //retrieve saved mapping
    controller.networkmanager.IncomingMessageQueue.MessageQueueListener core_listener =
      (controller.networkmanager.IncomingMessageQueue.MessageQueueListener)registrations.remove( listener );
    
    if( core_listener != null ) {
      core_queue.cancelQueueListener( core_listener );
    }
  }
  
  
  public void notifyOfExternalReceive( Message message ) throws IOException{
    if( message instanceof MessageAdapter ) {
      //the message must have been originally created by core and wrapped
      //so just use original core message...i.e. unwrap out of MessageAdapter
      core_queue.notifyOfExternallyReceivedMessage( ((MessageAdapter)message).getCoreMessage() );
      return;
    }
    
    //message originally created by plugin
    core_queue.notifyOfExternallyReceivedMessage( new MessageAdapter( message ) );
  }
  
  public int getPercentDoneOfCurrentMessage() {
    return core_queue.getPercentDoneOfCurrentMessage();
  }
  
}
