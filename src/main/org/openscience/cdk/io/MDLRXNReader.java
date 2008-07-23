/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2007  The Chemistry Development Kit (CDK) project
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
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
package org.openscience.cdk.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.interfaces.IReactionSet;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.formats.MDLRXNFormat;
import org.openscience.cdk.tools.LoggingTool;

/**
 * Reads a molecule from an MDL RXN file {@cdk.cite DAL92}.
 *
 * @cdk.module io
 * @cdk.svnrev  $Revision$
 *
 * @author     Egon Willighagen
 * @cdk.created    2003-07-24
 *
 * @cdk.keyword    file format, MDL RXN
 */
public class MDLRXNReader extends DefaultChemObjectReader {

    BufferedReader input = null;
    private LoggingTool logger = null;

    /**
     * Contructs a new MDLReader that can read Molecule from a given Reader.
     *
     * @param  in  The Reader to read from
     */
    public MDLRXNReader(Reader in) {
    	this(in, Mode.RELAXED);
    }
    public MDLRXNReader(Reader in, Mode mode) {
        logger = new LoggingTool(this);
        if (in instanceof BufferedReader) {
        	input = (BufferedReader)in;
        } else {
        	input = new BufferedReader(in);
        }
        super.mode = mode;
    }

    public MDLRXNReader(InputStream input) {
    	this(input, Mode.RELAXED);
    }
    public MDLRXNReader(InputStream input, Mode mode) {
        this(new InputStreamReader(input), mode);
    }
    
    public MDLRXNReader() {
        this(new StringReader(""));
    }
    
    public IResourceFormat getFormat() {
        return MDLRXNFormat.getInstance();
    }

    public void setReader(Reader input) throws CDKException {
        if (input instanceof BufferedReader) {
            this.input = (BufferedReader)input;
        } else {
            this.input = new BufferedReader(input);
        }
    }
    
    public void setReader(InputStream input) throws CDKException {
        setReader(new InputStreamReader(input));
    }

	public boolean accepts(Class classObject) {
		Class[] interfaces = classObject.getInterfaces();
		for (int i=0; i<interfaces.length; i++) {
			if (IChemModel.class.equals(interfaces[i])) return true;
			if (IChemFile.class.equals(interfaces[i])) return true;
			if (IReaction.class.equals(interfaces[i])) return true;
			if (IReactionSet.class.equals(interfaces[i])) return true;
		}
		return false;
	}

   /**
     * Takes an object which subclasses IChemObject, e.g.Molecule, and will read
     * this (from file, database, internet etc). If the specific implementation
     * does not support a specific IChemObject it will throw an Exception.
     *
     * @param  object                              The object that subclasses
     *      IChemObject
     * @return                                     The IChemObject read
     * @exception  CDKException
     */
     public IChemObject read(IChemObject object) throws CDKException {
         if (object instanceof IChemFile) {
        	 return readChemFile((IChemFile)object);
         } else if (object instanceof IChemModel) {
        	 return readChemModel((IChemModel)object);
         } else if (object instanceof IReactionSet) {
             return readReactionSet((IReactionSet)object);
         } else if (object instanceof IReaction) {
             return readReaction(object.getBuilder());
         } else {
             throw new CDKException("Only supported are Reaction, ReactionSet, ChemModel and ChemFile, and not " +
                 object.getClass().getName() + "."
             );
         }
     }
     
     public boolean accepts(IChemObject object) {
         if (object instanceof IReaction) {
             return true;
         } else if (object instanceof IChemModel) {
             return true;
         } else if (object instanceof IChemFile) {
             return true;
         } else if (object instanceof IReactionSet) {
             return true;
         }
         return false;
     }

     /**
 	 * Read a ChemFile from a file in MDL RDF format.
 	 *
 	 * @param  chemFile The IChemFile
 	 * @return          The IChemFile that was read from the RDF file.
 	 */
     private IChemFile readChemFile(IChemFile chemFile) throws CDKException {
         IChemSequence chemSequence = chemFile.getBuilder().newChemSequence();
         
         IChemModel chemModel = chemFile.getBuilder().newChemModel();
         chemSequence.addChemModel(readChemModel(chemModel));
         chemFile.addChemSequence(chemSequence);
 		return chemFile;
 	 }
     /**
  	 * Read a IChemModel from a file in MDL RDF format.
  	 *
  	 * @param  chemModel The IChemModel
  	 * @return           The IChemModel that was read from the RDF file
  	 */
      private IChemModel readChemModel(IChemModel chemModel) throws CDKException {
    	  IReactionSet setOfReactions = chemModel.getReactionSet();
          if (setOfReactions == null) {
         	 setOfReactions = chemModel.getBuilder().newReactionSet();
          }
          chemModel.setReactionSet(readReactionSet(setOfReactions));
          return chemModel;
      }
     /**
 	 * Read a IReactionSet from a file in MDL RDF format.
 	 *
 	 * @param  setOfReactions The IReactionSet
 	 * @return                The IReactionSet that was read from the RDF file
 	 */
     private IReactionSet readReactionSet(IReactionSet setOfReactions) throws CDKException {

         IReaction r = readReaction(setOfReactions.getBuilder());
 		  if (r != null) {
 			setOfReactions.addReaction(r);
 	      }
 		  
         String str;
         try {
             String line;
             while ((line = input.readLine()) != null) {
                 logger.debug("line: ", line);
                 // apparently, this is a SDF file, continue with 
                 // reading mol files
		 		str = new String(line);
		 		if (str.equals("$$$$")) {
		 		    r = readReaction(setOfReactions.getBuilder());
		 		    
		 		    if (r != null) {
			 			setOfReactions.addReaction(r);
		 		    }
		 		} else {
		 		    // here the stuff between 'M  END' and '$$$$'
		 		    if (r != null) {
			 			// ok, the first lines should start with '>'
			 			String fieldName = null;
			 			if (str.startsWith("> ")) {
			 			    // ok, should extract the field name
			 			    str.substring(2); // String content = 
			 			    int index = str.indexOf("<");
			 			    if (index != -1) {
				 				int index2 = str.substring(index).indexOf(">");
				 				if (index2 != -1) {
				 				    fieldName = str.substring(index+1,index+index2);
				 				}
			 			    }
			 			    // end skip all other lines
			 			    while ((line = input.readLine()) != null && line.startsWith(">")) {
			                     logger.debug("data header line: ", line);
			 			    }
			 			 }
			             if (line == null) {
			                 throw new CDKException("Expecting data line here, but found null!");
			             }
			 			String data = line;
			 			while ((line = input.readLine()) != null &&
			 			       line.trim().length() > 0) {
			                 if (line.equals("$$$$")) {
			                 	logger.error("Expecting data line here, but found end of molecule: ", line);
			                 	break;
			                 }
			                 logger.debug("data line: ", line);
			 			    data += line;
			 			    // preserve newlines, unless the line is exactly 80 chars; in that case it
			 			    // is assumed to continue on the next line. See MDL documentation.
			 			    if (line.length() < 80) data += System.getProperty("line.separator");
			 			}
			 			if (fieldName != null) {
			 			    logger.info("fieldName, data: ", fieldName, ", ", data);
			 			    r.setProperty(fieldName, data);
			 			}
		 		    }
		 		}
             }
         } catch (CDKException cdkexc) {
             throw cdkexc;
         } catch (Exception exception) {
             String error = "Error while parsing SDF";
             logger.error(error);
             logger.debug(exception);
             throw new CDKException(error, exception);
         }
         
         return setOfReactions;
     }
    /**
     * Read a Reaction from a file in MDL RXN format
     *
     * @return  The Reaction that was read from the MDL file.
     */
    private IReaction readReaction(IChemObjectBuilder builder) throws CDKException {
    	logger.debug("Reading new reaction");
        int linecount = 0;
    	IReaction reaction = builder.newReaction();
        try {
            input.readLine(); // first line should be $RXN
            input.readLine(); // second line
            input.readLine(); // third line
            input.readLine(); // fourth line
        } catch (IOException exception) {
            logger.debug(exception);
            throw new CDKException("Error while reading header of RXN file", exception);
        }

        int reactantCount = 0;
        int productCount = 0;
        try {
            String countsLine = input.readLine();
            linecount++;
            if (countsLine == null) {
                return null;
            }
            logger.debug("Line " + linecount + ": " + countsLine);
            if (countsLine.startsWith("$$$$")) {
                logger.debug("File is empty, returning empty reaction");
                return reaction;
            }
            /* this line contains the number of reactants
               and products */
            StringTokenizer tokenizer = new StringTokenizer(countsLine);
            reactantCount = Integer.valueOf(tokenizer.nextToken()).intValue();
            logger.info("Expecting " + reactantCount + " reactants in file");
            productCount = Integer.valueOf(tokenizer.nextToken()).intValue();
            logger.info("Expecting " + productCount + " products in file");
        } catch (Exception exception) {
            logger.debug(exception);
            throw new CDKException("Error while counts line of RXN file", exception);
        }
        
        // now read the reactants
        try {
            for (int i=1; i<=reactantCount; i++) {
                StringBuffer molFile = new StringBuffer();
                input.readLine(); // announceMDLFileLine
                String molFileLine = "";
                do {
                    molFileLine = input.readLine();
                    molFile.append(molFileLine);
                    molFile.append(System.getProperty("line.separator"));
                } while (!molFileLine.equals("M  END"));
                
                // read MDL molfile content
                MDLReader reader = new MDLReader(
                  new StringReader(molFile.toString()));
                IMolecule reactant = (IMolecule)reader.read(
                  builder.newMolecule()
                );
                  
                // add reactant
                reaction.addReactant(reactant);
            }
        } catch (CDKException exception) {
            // rethrow exception from MDLReader
            throw exception;
        } catch (Exception exception) {
            logger.debug(exception);
            throw new CDKException("Error while reading reactant", exception);
        }
        
        // now read the products
        try {
            for (int i=1; i<=productCount; i++) {
                StringBuffer molFile = new StringBuffer();
                input.readLine(); // String announceMDLFileLine = 
                String molFileLine = "";
                do {
                    molFileLine = input.readLine();
                    molFile.append(molFileLine);
                    molFile.append(System.getProperty("line.separator"));
                } while (!molFileLine.equals("M  END"));
                
                // read MDL molfile content
                MDLReader reader = new MDLReader(
                    new StringReader(molFile.toString()),
                    super.mode
                );
                IMolecule product = (IMolecule)reader.read(
                  builder.newMolecule());
                  
                // add reactant
                reaction.addProduct(product);
            }
        } catch (CDKException exception) {
            // rethrow exception from MDLReader
            throw exception;
        } catch (Exception exception) {
            logger.debug(exception);
            throw new CDKException("Error while reading products", exception);
        }
        
        // now try to map things, if wanted
        logger.info("Reading atom-atom mapping from file");
        // distribute all atoms over two AtomContainer's
        IAtomContainer reactingSide = builder.newAtomContainer();
        Iterator molecules = reaction.getReactants().molecules();
        while (molecules.hasNext()) {
            reactingSide.add((IMolecule)molecules.next());
        }
        IAtomContainer producedSide = builder.newAtomContainer();
        molecules = reaction.getProducts().molecules();
        while (molecules.hasNext()) {
            producedSide.add((IMolecule)molecules.next());
        }
        
        // map the atoms
        int mappingCount = 0;
//        IAtom[] reactantAtoms = reactingSide.getAtoms();
//        IAtom[] producedAtoms = producedSide.getAtoms();
        for (int i=0; i<reactingSide.getAtomCount(); i++) {
            for (int j=0; j<producedSide.getAtomCount(); j++) {
            	IAtom eductAtom = reactingSide.getAtom(i);
            	IAtom productAtom = producedSide.getAtom(j);
                if (eductAtom.getID() != null &&
                		eductAtom.getID().equals(productAtom.getID())) {
                    reaction.addMapping(
                        builder.newMapping(eductAtom, productAtom)
                    );
                    mappingCount++;
                    break;
                }
            }
        }
        logger.info("Mapped atom pairs: " + mappingCount);
        
        return reaction;
    }
    
    public void close() throws IOException {
        input.close();
    }
}

