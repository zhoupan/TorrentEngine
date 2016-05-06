/*
 * File    : TorrentImpl.java
 * Created : 08-Dec-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package pluginsimpl.local.torrent;

import torrentlib.Debug;
import torrentlib.Constants;
import plugins.torrent.TorrentAnnounceURLList;
import plugins.torrent.TorrentFile;
import plugins.torrent.TorrentException;
import plugins.torrent.Torrent;
import plugins.torrent.TorrentEncodingException;
import plugins.download.DownloadException;
import torrentlib.BEncoder;
import torrentlib.util.TorrentUtils;
import torrentlib.util.FileUtil;
import torrentlib.data.torrent.TOTorrentException;
import torrentlib.data.torrent.TOTorrent;
import torrentlib.data.torrent.TOTorrentFactory;
import torrentlib.data.torrent.TOTorrentFile;
import torrentlib.internat.LocaleTorrentUtil;
import torrentlib.internat.LocaleUtilEncodingException;
import torrentlib.internat.LocaleUtilDecoder;
import pluginsimpl.local.download.DownloadManagerImpl;
import pluginsimpl.local.download.DownloadImpl;
import java.net.*;
import java.util.Map;
import java.io.File;

import xfer.download.DownloadManagerState;
import xfer.download.DownloadManagerStateFactory;
import torrentlib.util.logging.LogRelation;
import torrentlib.util.platform.win32.PluginInterface;
import pluginsimpl.local.utils.UtilitiesImpl;

import torrentlib.AzureusCoreFactory;
import plugins.magnet.MagnetPlugin;

public class 
TorrentImpl
	extends LogRelation
	implements Torrent
{
	private static MagnetPlugin		magnet_plugin;
	
	private PluginInterface			pi;
	private TOTorrent				torrent;
	private LocaleUtilDecoder		decoder;
	
	private boolean					complete;
	
	public
	TorrentImpl(
		TOTorrent		_torrent )
	{
		this( null, _torrent );
	}
	
	public
	TorrentImpl(
		PluginInterface	_pi,
		TOTorrent		_torrent )
	{
		pi		= _pi;
		torrent	= _torrent;
	}
	
	public String
	getName()
	{
		String utf8Name = torrent.getUTF8Name();
		String	name = utf8Name == null ? decode( torrent.getName()) : utf8Name;
		
		name = FileUtil.convertOSSpecificChars( name, false );

		return( name );
	}
	
	public URL
	getAnnounceURL()
	{
		return( torrent.getAnnounceURL());
	}
	
	public void
	setAnnounceURL(
		URL		url )
	{
		torrent.setAnnounceURL( url );
		
		updated();
	}

	public TorrentAnnounceURLList
	getAnnounceURLList()
	{
		return( new TorrentAnnounceURLListImpl( this ));
	}

	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public boolean
	isDecentralised()
	{
		return( TorrentUtils.isDecentralised( torrent ));
	}
	public boolean
	isDecentralisedBackupEnabled()
	{
		return( TorrentUtils.getDHTBackupEnabled( torrent ));
	}
	
	public void
	setDecentralisedBackupRequested(
		boolean	requested )
	{
		TorrentUtils.setDHTBackupRequested( torrent, requested );
	}
	
	public boolean
	isDecentralisedBackupRequested()
	{
		return( TorrentUtils.isDHTBackupRequested( torrent ));

	}
	public boolean
	isPrivate()
	{
		return( TorrentUtils.getPrivate( torrent ));
	}
	
	public void
	setPrivate(
		boolean	priv )
	{
		TorrentUtils.setPrivate( torrent, priv );
	}
	
	public boolean
	wasCreatedByUs()
	{
		return( TorrentUtils.isCreatedTorrent( torrent ));
	}
	
	public URL
	getMagnetURI()
	
		throws TorrentException
	{
		if ( magnet_plugin == null ){
			
			PluginInterface magnet_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( MagnetPlugin.class );
			
			if ( magnet_pi != null ){
			
				 magnet_plugin = (MagnetPlugin)magnet_pi.getPlugin();
			}
		}
		
		if ( magnet_plugin == null ){
			
			throw( new TorrentException( "MegnetPlugin unavailable" ));
		}
		
		try{
			URL	res = magnet_plugin.getMagnetURL(  torrent.getHash());
			
			return( res );
				
		}catch( TOTorrentException e ){
			
			throw( new TorrentException(e ));
		}
	}
	
	public byte[]
	getHash()
	{
		try{
			return( torrent.getHash());
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace( e );
			
			return( null );
		}
	}
		
	public long
	getSize()
	{
		return( torrent.getSize());
	}
	
	public String
	getComment()
	{
		return( decode(torrent.getComment()));
	}
	
	public void
	setComment(
		String	comment )
	{
		torrent.setComment( comment );
	}
	public long
	getCreationDate()
	{
		return( torrent.getCreationDate());
	}
	
	public String
	getCreatedBy()
	{
		return( decode( torrent.getCreatedBy()));
	}
	

	public long
	getPieceSize()
	{
		return( torrent.getPieceLength());
	}
	
	public long
	getPieceCount()
	{
		return( torrent.getNumberOfPieces());
	}
	
	public byte[][]
  	getPieces()
  	{
		try{
			return( torrent.getPieces());
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace( e );
			
			return( new byte[0][0] );
		}
  	}	
	          	
	public TorrentFile[]
	getFiles()
	{
		TOTorrentFile[]	files = torrent.getFiles();
		
		TorrentFile[]	res = new TorrentFile[files.length];
		
		for (int i=0;i<res.length;i++){
		
			TOTorrentFile	tf = files[i];
			
			byte[][]	comps = tf.getPathComponents();
			
			String	name = "";
			
			for (int j=0;j<comps.length;j++){
				
				String	comp = decode(comps[j]);
			
				comp = FileUtil.convertOSSpecificChars( comp, j != comps.length-1 );
				
				name += (j==0?"":File.separator)+comp;
			}
			
			res[i] = new TorrentFileImpl(name, tf.getLength());
		}
		
		return( res );
	}
	
	protected void
	getDecoder()
	{
			// We defer the getting of the decoder until it is required as this Torrent may have been 
			// created in order to simply remove additional properties from it before serialising it
			// Indeed, this was happening and unfortunately resulting in 1) the encoding being
			// serialised 2) the user being prompted for an encoding choice 
		
		try{
			decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );
			
		}catch( Throwable e ){
			
		}
	}
	
	public String
	getEncoding()
	{
		getDecoder();
		
		if ( decoder != null ){
			
			return( decoder.getName());
		}
		
		return( Constants.DEFAULT_ENCODING );
	}
	
	public void
	setEncoding(String encoding)throws TorrentEncodingException {		
		try {
			LocaleTorrentUtil.setTorrentEncoding(torrent, encoding);
		} catch(LocaleUtilEncodingException e) {
			throw new TorrentEncodingException("Failed to set the encoding",e);
		}
	}
	
	public void
	setDefaultEncoding() throws TorrentEncodingException {
		setEncoding(Constants.DEFAULT_ENCODING);
	}
	
	protected String
	decode(
		byte[]		data )
	{
		getDecoder();
		
		if ( data != null ){
			
			if ( decoder != null ){
				
				try{
					return( decoder.decodeString(data));
					
				}catch( Throwable e ){
				}
			}
			
			return( new String(data));
		}
		
		return( "" );
	}
	
	public Object
	getAdditionalProperty(
		String		name )
	{
		return( torrent.getAdditionalProperty( name ));
	}
	
	public Torrent
	removeAdditionalProperties()
	{
		try{
			TOTorrent	t = TOTorrentFactory.deserialiseFromMap(torrent.serialiseToMap());
					
			t.removeAdditionalProperties();
	
			return( new TorrentImpl( t ));
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace(e);
			
			return( this );
		}
	}

	public void
	setPluginStringProperty(
		String		name,
		String		value )
	{
		PluginInterface	p = pi;
		
		if ( p == null ){
		
			p = UtilitiesImpl.getPluginThreadContext();	
		}
		
		if ( p == null ){
			
			name = "<internal>." + name;
			
		}else{
			
			name = p.getPluginID() + "." + name;
		}
		
		TorrentUtils.setPluginStringProperty( torrent, name, value );
	}
	
	public String
	getPluginStringProperty(
		String		name )
	{
		PluginInterface	p = pi;
		
		if ( p == null ){
		
			p = UtilitiesImpl.getPluginThreadContext();	
		}
		
		if ( p == null ){
			
			name = "<internal>." + name;
			
		}else{
			
			name = p.getPluginID() + "." + name;
		}
		
		return( TorrentUtils.getPluginStringProperty( torrent, name ));
	}
	
	public void
	setMapProperty(
		String		name,
		Map			value )
	{
		TorrentUtils.setPluginMapProperty( torrent, name, value );
	}
	
	public Map
	getMapProperty(
		String		name )
	{
		return( TorrentUtils.getPluginMapProperty( torrent, name ));
	}
	
	public Map
	writeToMap()
	
		throws TorrentException
	{
		try{
			return( torrent.serialiseToMap());
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "Torrent::writeToMap: fails", e ));
		}
	}
	
	public byte[]
	writeToBEncodedData()
	
		throws TorrentException
	{
		try{
			Map	map = torrent.serialiseToMap();
			
			return( BEncoder.encode( map ));
			
		}catch( Throwable e ){
			
			throw( new TorrentException( "Torrent::writeToBEncodedData: fails", e ));
		}
	}	
	
	public void
	writeToFile(
		File		file )
	
		throws TorrentException
	{
		try{
				// don't use TorrentUtils.writeToFile as this updates the internal torrent
				// file reference an means that the torrent get's auto-written to the new
				// location in future, most likley NOT the desired behaviour
			
			torrent.serialiseToBEncodedFile( file );
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "Torrent::writeToFile: fails", e ));
		}
	}
  
 	protected void
	updated()
	{
		try{
			DownloadImpl dm = (DownloadImpl)DownloadManagerImpl.getDownloadStatic( torrent );
		
			if ( dm != null ){
			
				dm.torrentChanged();
			}
		}catch( DownloadException e ){
			
			// torrent may not be running
		}
	}
	
	public void
	save()
		throws TorrentException
	{
		try{
			TorrentUtils.writeToFile( torrent );
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "Torrent::save Fails", e ));
		}	
	}
	
	public void
	setComplete(
		File		data_dir )
	
		throws TorrentException
	{		
		try{
			LocaleTorrentUtil.setDefaultTorrentEncoding( torrent );
		
			DownloadManagerState	download_manager_state = 
				DownloadManagerStateFactory.getDownloadState( torrent ); 

			TorrentUtils.setResumeDataCompletelyValid( download_manager_state );

			download_manager_state.save();
			
			complete	= true;
			
		}catch( Throwable e ){
			
			throw( new TorrentException("encoding selection fails", e ));
		}
	}
	
	public boolean
	isComplete()
	{
			// TODO: could check the download state too I guess...
		
		return( complete );
	}
	
	
  // Pass LogRelation off to core objects

	/* (non-Javadoc)
	 * @see core3.logging.LogRelation#getLogRelationText()
	 */
	public String getRelationText() {
		return propogatedRelationText(torrent);
	}

	/* (non-Javadoc)
	 * @see core3.logging.LogRelation#getQueryableInterfaces()
	 */
	public Object[] getQueryableInterfaces() {
		return new Object[] { torrent };
	}
	
	public boolean isSimpleTorrent() {return torrent.isSimpleTorrent();}
	
	public Torrent 
	getClone() 
			
		throws TorrentException 
	{
		try{
			return( new TorrentImpl( TOTorrentFactory.deserialiseFromMap( torrent.serialiseToMap())));
			
		}catch( Throwable e ){
			
			throw( new TorrentException("Cloning fails", e ));
		}
	}
}