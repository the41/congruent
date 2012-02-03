/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.basho.congruent.operations;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.UnresolvedConflictException;
import com.basho.riak.client.http.response.RiakIORuntimeException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author roach
 */
public class Get extends RiakOperation {

    @Override
    public String execute() 
    {
    
        
        String bucketName = new String(Base64.decodeBase64(commandNode.get("bucket").getTextValue()));
        String key = new String(Base64.decodeBase64(commandNode.get("key").getTextValue()));
        
        try 
        {
            Bucket bucket = riakClient.fetchBucket(bucketName).execute();
            
            /* This gets a little weird because of how this interface is designed.
             * If there's siblings, it's going to throw an exception because we
             * aren't specifying a ConflictResolver (because we want to see the 
             * siblings). The siblings are contained inside the exception
             */
            
            List<IRiakObject> riakObjectList = new LinkedList<IRiakObject>();
            
            try
            {
                IRiakObject response = bucket.fetch(key).execute();
                if (null != response)
                    riakObjectList.add(response);
            }
            catch (UnresolvedConflictException urc)
            {
                for (IRiakObject ro : (Collection<IRiakObject>)urc.getSiblings())
                {
                    riakObjectList.add(ro);
                }
            }
            
            
            StringBuilder term = new StringBuilder("{ok,");
            
            
            if (riakObjectList.isEmpty())
            {
                term.append("not_found}.");
            }
            else
            {
                term.append("[");
                
                
                for ( IRiakObject rObj : riakObjectList) 
                {
                    term.append("<<");
                    for (byte b : rObj.getValue())
                    {
                        term.append((int)b);
                        term.append(",");
                    }
                    term.deleteCharAt(term.length() - 1);
                    term.append(">>,");

                }   

                term.deleteCharAt(term.length() - 1);

                               
                term.append("]}.");
            }
            
            return term.toString();
            
        }
        catch (RiakRetryFailedException ex)
        {
            return "{error,\"" + ex.getMessage() + "\"}.";
        }
        catch (RiakIORuntimeException ex)
        {
            return "{error,\"" + ex.getMessage() + "\"}.";
        }
        
    }
    
    
    
}
