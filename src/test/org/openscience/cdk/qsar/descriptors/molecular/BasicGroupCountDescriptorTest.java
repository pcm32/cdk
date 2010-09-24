/* Copyright (C) 2010  Egon Willighagen <egonw@users.sf.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA. 
 */
package org.openscience.cdk.qsar.descriptors.molecular;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.smiles.SmilesParser;

public class BasicGroupCountDescriptorTest extends MolecularDescriptorTest {

    @Before
    public void setUp() throws Exception {
        setDescriptor(BasicGroupCountDescriptor.class);
    }

    @Test public void testAmine() throws Exception {
        SmilesParser sp = new SmilesParser(
            NoNotificationChemObjectBuilder.getInstance()
        );
        IMolecule mol = sp.parseSmiles("NC");
        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(1, result.intValue());
    }
}

