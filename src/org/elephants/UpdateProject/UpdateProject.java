/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elephants.UpdateProject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;

import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.netbeans.api.project.Project;

import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.cnd.discovery.wizard.api.support.ProjectBridge;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationDescriptorProvider;
import org.netbeans.modules.cnd.makeproject.api.configurations.Folder;
import org.netbeans.modules.cnd.makeproject.api.configurations.Item;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfigurationDescriptor;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;

import org.openide.LifecycleManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

import org.openide.nodes.Node;

import org.openide.util.NbBundle.Messages;

@ActionID(
    category = "File",
id = "org.elephants.UpdateProject.UpdateProject")
@ActionRegistration(
    iconBase = "org/elephants/UpdateProject/bulb.png",
displayName = "#CTL_UpdateProject")
@ActionReference(path = "Toolbars/File", position = 500)
@Messages("CTL_UpdateProject=Update Project")
public final class UpdateProject implements ActionListener {

    private final Project context;
    

    public UpdateProject(Project context) {
        this.context = context;      
        
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        //ExternalUpdate();
        ProjectBridge projectBridge = new ProjectBridge(context);               
        ConfigurationDescriptorProvider cdp = context.getLookup().lookup(ConfigurationDescriptorProvider.class);
        MakeConfigurationDescriptor projectDescriptor = cdp.getConfigurationDescriptor();
        
        String path = FileUtil.toFile(context.getProjectDirectory()).getPath();
        
        ArrayList<File> list = new ArrayList<File>();
            
        BuscarCPP(list, path);
        
        System.out.println("Information: " + ProjectUtils.getInformation(context).getDisplayName());  

        Folder root = projectDescriptor.getLogicalFolders();
        Folder testRootFolder = null;
        for (Folder folder : root.getFolders()) {

            //folder.setDisplayName(folder.getDisplayName() + " trap");
            
            Item[] oldItems = folder.getItemsAsArray();
            
            if(folder.getDisplayName().equals("Source Files") || folder.getDisplayName().equals("Header Files"))
            {            
                for (Item oldItem : oldItems) {
                    folder.removeItem(oldItem);
                }
            }
            
            //System.out.println("Folder: " + folder.getDisplayName());
            
            if(folder.getDisplayName().equals("Source Files"))
            {              
                for (File file : list) {
                    
                    Item item = projectBridge.createItem(file.getPath());

                    item = folder.addItem(item);

                    ProjectBridge.setExclude(item, false);

                    //ProjectBridge.setHeaderTool(item);

                    //ProjectBridge.excludeItemFromOtherConfigurations(item);

                    System.out.println("Item añadido: " + item.getPath());
                }                
            }
            
            
        }
        
        
    }
    
    public void BuscarCPP(ArrayList<File> v, String dirName) {
        File f = new File(dirName);

        File[] directories = f.listFiles(new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();

                return (file.isDirectory() && !file.getName().startsWith("."));
            }
        });

        File[] files = f.listFiles(new FilenameFilter() {
            public boolean accept(File file, String name) {
                return (name.endsWith(".cpp") || name.endsWith(".h"));

            }
        });

        v.addAll(Arrays.asList(files));

        for (File directory : directories) {
            BuscarCPP(v, directory.getPath());
        }
    }

    private void ModificarXML(ArrayList<File> list, String xmlName) {

        try {

            SAXBuilder builder = new SAXBuilder();
            File xmlFile = new File(xmlName);

            Document doc = (Document) builder.build(xmlFile);
            Element rootNode = doc.getRootElement();

            //System.out.println(rootNode.getChildren());
            // update staff id attribute
            List<Element> logicalFolders = rootNode.getChild("logicalFolder").getChildren();

            //System.out.println(logicalFolders);


            for (Element element : logicalFolders) {

                element.removeChildren("itemPath");

                if (element.getAttributeValue("name", "none").equals("SourceFiles")) {
                    //System.out.println(element.getChildren());

                    for (File file : list) {
                        Element itemPath = new Element("itemPath");
                        itemPath.setText(file.getPath());
                        element.addContent(itemPath);
                    }
                }

            }
            
            /* Extructura del xml
             * configurationDescriptor
             *   confs
             *     conf
             *       item
             *     conf
             *       item
             */

            List<Element> confs = rootNode.getChild("confs").getChildren();

            for (Element element : confs) {

                element.removeChildren("item");

                if (element.getAttributeValue("name", "none").equals("Debug")
                 || element.getAttributeValue("name", "none").equals("Release")) {//devuelve "none" si no tiene un atributo name

                    System.out.println(element.getChildren());

                    for (File file : list) {
                        
                        //Formato: <item path="Plush.cpp" ex="false" tool="1" flavor2="0" />

                        Element item = new Element("item");
                        item.setAttribute("path", file.getPath());
                        item.setAttribute("ex", "false");

                        String tool = "1";
                        if (file.getName().endsWith(".h")) {
                            tool = "3";
                        }

                        item.setAttribute("tool", tool);
                        item.setAttribute("flavor2", "0");

                        element.addContent(item);
                    }
                }

            }



            XMLOutputter xmlOutput = new XMLOutputter();

            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(xmlName));

            // xmlOutput.output(doc, System.out);

            System.out.println("File updated!");

        } catch (JDOMException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException io) {
            //io.printStackTrace();
            System.err.println("Archivo configuration.xml no encontrado");
            System.err.println("Buscado en: " + xmlName);
            
        } 
        
    }

    private void ExternalUpdate() {
        try {

            ProjectManager.getDefault().clearNonProjectCache();
            
            System.out.println(ProjectUtils.getInformation(context));  
            
            Project project;
            Project updatedProject;


            project = context;
            
            Project[] array = new Project[1];
            array[0] = project;
            
            String path = FileUtil.toFile(project.getProjectDirectory()).getPath();
            ArrayList<File> list = new ArrayList<File>();
            
            BuscarCPP(list, path);
            
            ModificarXML(list, path + "/nbproject/configurations.xml");
            
            
            File projectToBeOpenedFile = new File(path);
            FileObject projectToBeOpened = FileUtil.toFileObject(projectToBeOpenedFile);
            
        
            updatedProject = ProjectManager.getDefault().findProject(projectToBeOpened);
            array[0] = updatedProject;
            ProjectManager.getDefault().clearNonProjectCache();
            OpenProjects.getDefault().close(array); 
            System.err.println("Proyecto cerrado");
            ProjectManager.getDefault().clearNonProjectCache();
            OpenProjects.getDefault().open(array, false);       
            
            System.err.println("Proyecto abierto");
          
            
            ProjectManager.getDefault().clearNonProjectCache();
            
    
            
            LifecycleManager.getDefault().markForRestart();
            LifecycleManager.getDefault().exit();
            
        
            
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
