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

import javax.vecmath.Point2d;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * @cdk.module test-qsarmolecular
 */
public class AcidicGroupCountDescriptorTest extends MolecularDescriptorTest {

    @Before
    public void setUp() throws Exception {
        setDescriptor(AcidicGroupCountDescriptor.class);
    }

    @Test public void testOneAcidGroup() throws Exception {
        SmilesParser sp = new SmilesParser(
            NoNotificationChemObjectBuilder.getInstance()
        );
        IMolecule mol = sp.parseSmiles("CC(=O)O");
        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(1, result.intValue());
    }

    @Test public void testSulphurAcidGroup() throws Exception {
        SmilesParser sp = new SmilesParser(
            NoNotificationChemObjectBuilder.getInstance()
        );
        IMolecule mol = sp.parseSmiles("OS(=O)(=O)O");
        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(2, result.intValue());
    }

    @Test public void testPhosphorusAcidGroup() throws Exception {
        SmilesParser sp = new SmilesParser(
            NoNotificationChemObjectBuilder.getInstance()
        );
        IMolecule mol = sp.parseSmiles("O=P(=O)O");
        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(1, result.intValue());
    }

    @Test public void testFancyGroup() throws Exception {
        SmilesParser sp = new SmilesParser(
            NoNotificationChemObjectBuilder.getInstance()
        );
        IMolecule mol = sp.parseSmiles("[NH](S(=O)=O)C(F)(F)F");
        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(1, result.intValue());
    }

    @Test public void testNitroRing() throws Exception {
        SmilesParser sp = new SmilesParser(
            NoNotificationChemObjectBuilder.getInstance()
        );
        IMolecule mol = sp.parseSmiles("[nH]1nnnc1");
        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(1, result.intValue());
    }

    /**
     * @cdk.inchi InChI=1S/C2H2N4O2/c7-2(8)1-3-5-6-4-1/h(H,7,8)(H,3,4,5,6)
     */
    @Test public void testTwoGroup() throws Exception {
        IMolecule mol = new Molecule();
        IAtom a1 = mol.getBuilder().newInstance(IAtom.class,"O");
        a1.setFormalCharge(0);
        a1.setPoint2d(new Point2d(5.9019, 0.5282));
        mol.addAtom(a1);
        IAtom a2 = mol.getBuilder().newInstance(IAtom.class,"O");
        a2.setFormalCharge(0);
        a2.setPoint2d(new Point2d(5.3667, -1.1191));
        mol.addAtom(a2);
        IAtom a3 = mol.getBuilder().newInstance(IAtom.class,"N");
        a3.setFormalCharge(0);
        a3.setPoint2d(new Point2d(3.3987, -0.4197));
        mol.addAtom(a3);
        IAtom a4 = mol.getBuilder().newInstance(IAtom.class,"N");
        a4.setFormalCharge(0);
        a4.setPoint2d(new Point2d(2.5896, 0.1681));
        mol.addAtom(a4);
        IAtom a5 = mol.getBuilder().newInstance(IAtom.class,"N");
        a5.setFormalCharge(0);
        a5.setPoint2d(new Point2d(3.8987, 1.1191));
        mol.addAtom(a5);
        IAtom a6 = mol.getBuilder().newInstance(IAtom.class,"N");
        a6.setFormalCharge(0);
        a6.setPoint2d(new Point2d(2.8987, 1.1191));
        mol.addAtom(a6);
        IAtom a7 = mol.getBuilder().newInstance(IAtom.class,"C");
        a7.setFormalCharge(0);
        a7.setPoint2d(new Point2d(4.2077, 0.1681));
        mol.addAtom(a7);
        IAtom a8 = mol.getBuilder().newInstance(IAtom.class,"C");
        a8.setFormalCharge(0);
        a8.setPoint2d(new Point2d(5.1588, -0.141));
        mol.addAtom(a8);
        IAtom a9 = mol.getBuilder().newInstance(IAtom.class,"H");
        a9.setFormalCharge(0);
        a9.setPoint2d(new Point2d(2.0, -0.0235));
        mol.addAtom(a9);
        IAtom a10 = mol.getBuilder().newInstance(IAtom.class,"H");
        a10.setFormalCharge(0);
        a10.setPoint2d(new Point2d(6.4916, 0.3366));
        mol.addAtom(a10);
        IBond b1 = mol.getBuilder().newInstance(IBond.class,a1, a8, IBond.Order.SINGLE);
        mol.addBond(b1);
        IBond b2 = mol.getBuilder().newInstance(IBond.class,a1, a10, IBond.Order.SINGLE);
        mol.addBond(b2);
        IBond b3 = mol.getBuilder().newInstance(IBond.class,a2, a8, IBond.Order.DOUBLE);
        mol.addBond(b3);
        IBond b4 = mol.getBuilder().newInstance(IBond.class,a3, a4, IBond.Order.SINGLE);
        mol.addBond(b4);
        IBond b5 = mol.getBuilder().newInstance(IBond.class,a3, a7, IBond.Order.DOUBLE);
        mol.addBond(b5);
        IBond b6 = mol.getBuilder().newInstance(IBond.class,a4, a6, IBond.Order.SINGLE);
        mol.addBond(b6);
        IBond b7 = mol.getBuilder().newInstance(IBond.class,a4, a9, IBond.Order.SINGLE);
        mol.addBond(b7);
        IBond b8 = mol.getBuilder().newInstance(IBond.class,a5, a6, IBond.Order.DOUBLE);
        mol.addBond(b8);
        IBond b9 = mol.getBuilder().newInstance(IBond.class,a5, a7, IBond.Order.SINGLE);
        mol.addBond(b9);
        IBond b10 = mol.getBuilder().newInstance(IBond.class,a7, a8, IBond.Order.SINGLE);
        mol.addBond(b10);

        IntegerResult result =
            (IntegerResult)descriptor.calculate(mol).getValue();
        Assert.assertEquals(2, result.intValue());
      }

}

