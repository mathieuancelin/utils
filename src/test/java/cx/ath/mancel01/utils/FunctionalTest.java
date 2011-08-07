/*
 *  Copyright 2011 Mathieu ANCELIN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package cx.ath.mancel01.utils;

import org.junit.Test;
import java.util.List;

import junit.framework.Assert;
import static cx.ath.mancel01.utils.C.*;
import static cx.ath.mancel01.utils.F.*;

public class FunctionalTest {
    
    @Test
    public void testQuickSort() {
        List<Integer> values = eList( 7 )._( 4 )._( 12 )._( 2 )._( 9 )._( 1 )._( 5 )._( 8 );
        List<Integer> expected = eList( 1 )._( 2 )._( 4 )._( 5 )._( 7 )._( 8 )._( 9 )._( 12 );
        Assert.assertEquals( expected, quickSort( values ) );
    }
    
    public static List<Integer> quickSort( List<Integer> pxs ) {
        EnhancedList<Integer> xs = eList( pxs );
        if ( xs.size() <= 1 ) {
            return xs;
        } else {
            final int pivot = xs.get( xs.size() / 2 );
            return 
                eList( quickSort( xs.filter( new Function<Integer, Boolean>() {
                    public Boolean apply( Integer t ) { return pivot > t; }})))
                    
                ._( xs.filter( new Function<Integer, Boolean>() {
                    public Boolean apply( Integer t ) { return pivot == t; }}))
                    
                ._( quickSort( xs.filter( new Function<Integer, Boolean>() { 
                    public Boolean apply( Integer t ) { return pivot < t; }})));
        }
    }
}
