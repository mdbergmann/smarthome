/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.io.rest.core.service.test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

import javax.ws.rs.core.Response

import org.eclipse.smarthome.io.rest.core.service.ConfigurableServiceResource
import org.eclipse.smarthome.test.OSGiTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.osgi.service.cm.Configuration
import org.osgi.service.cm.ConfigurationAdmin


/**
 * ConfigurableServiceResourceOSGiTest tests the ConfigurableService REST resource on the OSGi level.
 *
 * @author Dennis Nobel - Initial contribution
 */
class ConfigurableServiceResourceOSGiTest extends OSGiTest {

    ConfigurableServiceResource configurableServiceResource

    def properties = null;

    @Before
    void setUp() {
        def configAdmin = [
            getConfiguration: { pid ->
                [
                    getProperties: { properties },
                    update: { propertiesArg -> properties = propertiesArg },
                    delete: { properties = null }
                ] as Configuration
            }
        ] as ConfigurationAdmin
        registerService configAdmin

        configurableServiceResource = getService ConfigurableServiceResource
        assertThat configurableServiceResource, is(not(null))
    }

    @After
    void cleanUp() {
    }


    @Test
    void 'assert getConfigurableServices works'() {
        def configurableService  = {} as SomeServiceInterface
        registerService(configurableService, [
            "service.pid": "pid",
            "service.config.description.uri": "someuri",
            "service.config.label": "label",
            "service.config.category": "category"
        ] as Hashtable)

        def configurableServices = configurableServiceResource.getAll()
        assertThat configurableServices.size(), is(1)
        assertThat configurableServices[0].id, is(equalTo("pid"))
        assertThat configurableServices[0].configDescriptionURI, is(equalTo("someuri"))
        assertThat configurableServices[0].label, is(equalTo("label"))
        assertThat configurableServices[0].category, is(equalTo("category"))
    }

    @Test
    void 'assert component name fallback works'() {
        def configurableService  = {} as SomeServiceInterface
        registerService(configurableService, [
            "component.name": "component.name",
            "service.config.description.uri": "someuri",
        ] as Hashtable)

        def configurableServices = configurableServiceResource.getAll()
        assertThat configurableServices.size(), is(1)
        assertThat configurableServices[0].id, is(equalTo("component.name"))
    }

    @Test
    void 'assert configuration management works'() {

        Response response = configurableServiceResource.getConfiguration("id")
        assertThat response.status, is(404)

        response = configurableServiceResource.updateConfiguration("id", ["a" : "b"])
        assertThat response.status, is(204)

        response = configurableServiceResource.getConfiguration("id")
        assertThat response.status, is(200)
        assertThat response.entity.get("a"), is(equalTo("b"))

        response = configurableServiceResource.updateConfiguration("id", ["a" : "c"])
        assertThat response.status, is(200)
        assertThat response.entity.get("a"), is(equalTo("b"))

        response = configurableServiceResource.deleteConfiguration("id")
        assertThat response.status, is(200)
        assertThat response.entity.get("a"), is(equalTo("c"))

        response = configurableServiceResource.getConfiguration("id")
        assertThat response.status, is(404)

        response = configurableServiceResource.deleteConfiguration("id")
        assertThat response.status, is(204)
    }

    private interface SomeServiceInterface {}
}
