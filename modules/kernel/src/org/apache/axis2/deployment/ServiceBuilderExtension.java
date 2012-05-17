package org.apache.axis2.deployment;

import java.util.Map;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.AxisService;

/**
 * <p>
 * The interface ServiceBuilderExtension provides an extension point to
 * org.apache.axis2.deployment.Deployer interface. During the deployment, a
 * Deployer invoke all ServiceBuilderExtension instances associated with it and
 * receive a list of org.apache.axis2.description.AxisService generated by one
 * of above ServiceBuilderExtension instances.
 * </p>
 * 
 * <p>
 * It is possible to register ServiceBuilderExtension for a
 * org.apache.axis2.deployment.Deployer through the axis2.xml configuration file
 * or one can can set ServiceBuilderExtension programtically.
 * </p>
 * 
 * <p>
 * Example
 * </p>
 * <p>
 * 
 * <pre>
 * {@code
 *           <deployer extension="extension" directory="custom-directory" class="Deployer implementation">
 *              <serviceBuilderExtension name ="builderExtensionA" class="org.apache.axis2.BuilderExtensionA"/>
 *              <serviceBuilderExtension name ="builderExtensionA" class="org.apache.axis2.BuilderExtensionB"/>
 *              <serviceBuilderExtension name ="builderExtensionA" class="org.apache.axis2.BuilderExtensionC"/> 
 *      </deployer> }
 * </pre> 
 * </p>
 * 
 * 
 * <ul>
 * <li>
 * To use ServiceBuilderExtension a custom Deployer must be extended from org.apache.axis2.deployment.AbstractDeployer 
 * class where org.apache.axis2.deployment.AbstractDeployer class register ServiceBuilderExtensions with underline Deployer
 * instance. 
 * </li>
 * <li>
 * Within deploy() method it is expected to call org.apache.axis2.deployment.AbstractDeployer#executeServiceBuilderExtensions
 * method to receive  list of AxisService instances generated by ServiceBuilderExtensions.
 * </li>
 * <li>
 *  For a given deploymentFile, if a ServiceBuilderExtension could create a AxisService then 
 *  stop execution of other ServiceBuilderExtensions registered and return the AxisService immediately 
 *  to the base Deployer for the further processing.    
 * </li>
 * </ul>
 * 
 * @since 1.7.0
 */
public interface ServiceBuilderExtension {

    /**
     * Initialize ServiceBuilderExtension instance. This method should be called
     * when Initializing the base Deployer.
     * 
     * @param configurationContext
     *            the configuration context
     */
    public void init(ConfigurationContext configurationContext);

    /**
     * Builds the AxisService.
     * 
     * @param deploymentFileData
     *            the deployment file data *
     * @return the map of AxisService instances.
     * @throws DeploymentException
     *             the deployment exception
     */
    public Map<String, AxisService> buildAxisServices(DeploymentFileData deploymentFileData)
            throws DeploymentException;
    
    public void setDirectory(String directory);

}
