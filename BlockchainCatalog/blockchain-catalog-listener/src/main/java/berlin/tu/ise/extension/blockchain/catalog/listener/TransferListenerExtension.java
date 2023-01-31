/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.policy.spi.observe.PolicyDefinitionObservable;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.observe.asset.AssetObservable;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


public class TransferListenerExtension implements ServiceExtension {

    @Inject
    private TransferProcessObservable transferProcessObservable;

    // Needs to be injected to get Access to AssetObservable
    @Inject
    private AssetService assetService;

    // Needs to be injected to get Access to PolicyDefinitionObservable
    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private ContractDefinitionService contractDefinitionService;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private EventRouter eventRouter;


    @Override
    public void initialize(ServiceExtensionContext context) {


        //var assetObservable = ((AssetServiceImpl) assetService).observable;

        //var assetObservable = context.getService(AssetObservable.class);

        //var policyObservable = context.getService(PolicyDefinitionObservable.class);

        //var contractObservable = context.getService(ContractDefinitionObservable.class);

        var monitor = context.getMonitor();





        transferProcessObservable.registerListener(new MarkerFileCreator(monitor));
        BlockchainAssetCreator blockchainAssetCreator = new BlockchainAssetCreator(monitor, assetService, assetIndex);
        eventRouter.register(new BlockchainAssetCreationSubscriber(blockchainAssetCreator, monitor)); // asynchronous dispatch
        eventRouter.registerSync(new BlockchainAssetCreationSubscriber(blockchainAssetCreator, monitor)); // synchronous dispatch


        //assetObservable.registerListener(blockchainAssetCreator);

        //policyObservable.registerListener(new BlockchainPolicyCreator(monitor));

        String idsWebhookAddress = context.getSetting("ids.webhook.address", "http://localhost:8282");
        // append /api/v1/ids/data to the webhook address to get the IDS data endpoint
        idsWebhookAddress = idsWebhookAddress + "/api/v1/ids/data";
        //contractObservable.registerListener(new BlockchainContractCreator(monitor, idsWebhookAddress));


    }
}
