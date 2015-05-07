/*
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 *
 */

package com.pentaho.metaverse.api.analyzer.kettle.step;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.dictionary.MetaverseTransientNode;
import com.pentaho.metaverse.api.ChangeType;
import com.pentaho.metaverse.api.IAnalysisContext;
import com.pentaho.metaverse.api.IComponentDescriptor;
import com.pentaho.metaverse.api.IMetaverseBuilder;
import com.pentaho.metaverse.api.IMetaverseNode;
import com.pentaho.metaverse.api.MetaverseAnalyzerException;
import com.pentaho.metaverse.api.MetaverseObjectFactory;
import com.pentaho.metaverse.api.StepField;
import com.pentaho.metaverse.api.analyzer.kettle.ComponentDerivationRecord;
import com.pentaho.metaverse.api.model.Operation;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rfellows on 5/14/15.
 */
@RunWith( MockitoJUnitRunner.class )
public class StepAnalyzerTest {

  StepAnalyzer analyzer;

  Set<StepField> usedFields;

  StepNodes inputs;
  StepNodes outputs;

  @Mock IMetaverseNode fieldNode;
  @Mock IMetaverseNode rootNode;
  @Mock IMetaverseBuilder builder;
  @Mock BaseStepMeta baseStepMeta;
  @Mock IComponentDescriptor descriptor;
  @Mock IAnalysisContext context;
  @Mock StepMeta parentStepMeta;
  @Mock TransMeta parentTransMeta;

  @Before
  public void setUp() throws Exception {
    StepAnalyzer stepAnalyzer = new StepAnalyzer() {
      @Override protected Set<StepField> getUsedFields( BaseStepMeta meta ) {
        return null;
      }
      @Override protected void customAnalyze( BaseStepMeta meta, IMetaverseNode rootNode )
        throws MetaverseAnalyzerException {
      }
      @Override public Set<Class<? extends BaseStepMeta>> getSupportedSteps() {
        return null;
      }
    };
    analyzer = spy( stepAnalyzer );
    analyzer.rootNode = rootNode;
    analyzer.baseStepMeta = baseStepMeta;
    analyzer.descriptor = descriptor;
    analyzer.parentTransMeta = parentTransMeta;
    analyzer.parentStepMeta = parentStepMeta;
    usedFields = new HashSet<>();

    inputs = new StepNodes();
    inputs.addNode( "prevStep", "name", fieldNode );
    inputs.addNode( "prevStep", "address", fieldNode );
    inputs.addNode( "prevStep", "email", fieldNode );
    inputs.addNode( "prevStep", "birthday", fieldNode );
    inputs.addNode( "prevStep2", "employeeNumber", fieldNode );
    inputs.addNode( "prevStep2", "occupation", fieldNode );

    outputs = new StepNodes();
    outputs.addNode( "nextStep", "full name", fieldNode );
    outputs.addNode( "nextStep", "address", fieldNode );
    outputs.addNode( "nextStep", "email", fieldNode );
    outputs.addNode( "nextStep", "date of birth", fieldNode );
    outputs.addNode( "nextStep", "ID", fieldNode );
    outputs.addNode( "nextStep", "occupation", fieldNode );

    doReturn( builder ).when( analyzer ).getMetaverseBuilder();
    MetaverseObjectFactory factory = new MetaverseObjectFactory();
    doReturn( factory ).when( analyzer ).getMetaverseObjectFactory();

    when( descriptor.getContext() ).thenReturn( context );
    when( parentStepMeta.getName() ).thenReturn( "STEP NAME" );
  }

  @Test
  public void testProcessUsedFields() throws Exception {
    StepField name = new StepField( "prevStep", "name" );
    StepField address = new StepField( "prevStep", "birthday" );
    StepField empNum = new StepField( "prevStep2", "employeeNumber" );
    usedFields.add( name );
    usedFields.add( address );
    usedFields.add( empNum );
    doReturn( inputs ).when( analyzer ).getInputs();

    analyzer.processUsedFields( usedFields );

    verify( builder, times( usedFields.size() ) ).addLink( rootNode, DictionaryConst.LINK_USES, fieldNode );
  }

  @Test
  public void testGetChanges() throws Exception {
    ComponentDerivationRecord change1 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord change2 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord change3 = mock( ComponentDerivationRecord.class );
    Set<ComponentDerivationRecord> changeRecords = new HashSet<>();
    changeRecords.add( change1 );
    changeRecords.add( change2 );
    changeRecords.add( change3 );

    ComponentDerivationRecord passthrough1 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord passthrough2 = mock( ComponentDerivationRecord.class );
    Set<ComponentDerivationRecord> passthroughs = new HashSet<>();
    passthroughs.add( passthrough1 );
    passthroughs.add( passthrough2 );

    doReturn( changeRecords ).when( analyzer ).getChangeRecords( baseStepMeta );
    doReturn( passthroughs ).when( analyzer ).getPassthroughChanges();

    Set<ComponentDerivationRecord> changes = analyzer.getChanges();

    assertTrue( CollectionUtils.isNotEmpty( changes ) );
    assertEquals( 5, changes.size() );
  }

  @Test
  public void testGetChanges_noChangeRecords() throws Exception {
    ComponentDerivationRecord passthrough1 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord passthrough2 = mock( ComponentDerivationRecord.class );
    Set<ComponentDerivationRecord> passthroughs = new HashSet<>();
    passthroughs.add( passthrough1 );
    passthroughs.add( passthrough2 );

    doReturn( null ).when( analyzer ).getChangeRecords( baseStepMeta );
    doReturn( passthroughs ).when( analyzer ).getPassthroughChanges();

    Set<ComponentDerivationRecord> changes = analyzer.getChanges();

    assertTrue( CollectionUtils.isNotEmpty( changes ) );
    assertEquals( 2, changes.size() );
  }
  @Test
  public void testGetChanges_ExceptionGettingChangeRecords() throws Exception {
    ComponentDerivationRecord passthrough1 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord passthrough2 = mock( ComponentDerivationRecord.class );
    Set<ComponentDerivationRecord> passthroughs = new HashSet<>();
    passthroughs.add( passthrough1 );
    passthroughs.add( passthrough2 );

    doThrow( MetaverseAnalyzerException.class ).when( analyzer ).getChangeRecords( baseStepMeta );
    doReturn( passthroughs ).when( analyzer ).getPassthroughChanges();

    Set<ComponentDerivationRecord> changes = analyzer.getChanges();

    assertTrue( CollectionUtils.isNotEmpty( changes ) );
    assertEquals( 2, changes.size() );
  }

  @Test
  public void testGetChanges_noPassthroughs() throws Exception {
    ComponentDerivationRecord change1 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord change2 = mock( ComponentDerivationRecord.class );
    ComponentDerivationRecord change3 = mock( ComponentDerivationRecord.class );
    Set<ComponentDerivationRecord> changeRecords = new HashSet<>();
    changeRecords.add( change1 );
    changeRecords.add( change2 );
    changeRecords.add( change3 );

    doReturn( changeRecords ).when( analyzer ).getChangeRecords( baseStepMeta );
    doReturn( null ).when( analyzer ).getPassthroughChanges();

    Set<ComponentDerivationRecord> changes = analyzer.getChanges();

    assertTrue( CollectionUtils.isNotEmpty( changes ) );
    assertEquals( 3, changes.size() );
  }

  @Test
  public void testGetPassthroughChanges_noInputs() throws Exception {
    Set passthroughChanges = analyzer.getPassthroughChanges();
    assertTrue( CollectionUtils.isEmpty( passthroughChanges ) );
  }

  @Test
  public void testGetPassthroughChanges() throws Exception {
    doReturn( inputs ).when( analyzer ).getInputs();

    // assume all inputs are passthroughs
    doReturn( true ).when( analyzer ).isPassthrough( any( StepField.class ) );

    Set<ComponentDerivationRecord> passthroughChanges = analyzer.getPassthroughChanges();
    assertTrue( CollectionUtils.isNotEmpty( passthroughChanges ) );

    assertEquals( inputs.getFieldNames().size(), passthroughChanges.size() );
    for ( ComponentDerivationRecord passthroughChange : passthroughChanges ) {
      // make sure the from field equals the to field
      assertEquals( passthroughChange.getChangedEntityName(), passthroughChange.getOriginalEntityName() );
    }
  }

  @Test
  public void testGetPassthroughChanges_none() throws Exception {
    doReturn( inputs ).when( analyzer ).getInputs();

    // assume no inputs are passthroughs
    doReturn( false ).when( analyzer ).isPassthrough( any( StepField.class ) );

    Set<ComponentDerivationRecord> passthroughChanges = analyzer.getPassthroughChanges();
    assertTrue( CollectionUtils.isEmpty( passthroughChanges ) );
  }

  @Test
  public void testIsPassthrough() throws Exception {
    doReturn( outputs ).when( analyzer ).getOutputs();

    StepField testField = new StepField( "previousStep2", "occupation" );
    assertTrue( analyzer.isPassthrough( testField ) );
  }

  @Test
  public void testIsPassthrough_false() throws Exception {
    doReturn( outputs ).when( analyzer ).getOutputs();

    StepField testField = new StepField( "previousStep2", "employeeNumber" );
    assertFalse( analyzer.isPassthrough( testField ) );
  }

  @Test
  public void testIsPassthrough_noOutputs() throws Exception {
    doReturn( null ).when( analyzer ).getOutputs();
    StepField testField = new StepField( "previousStep2", "occupation" );
    assertFalse( analyzer.isPassthrough( testField ) );
  }

  @Test
  public void testMapChange_nullChange() throws Exception {
    analyzer.mapChange( null );
    verify( analyzer, never() ).getInputs();
    verify( analyzer, never() ).getOutputs();
    verify( analyzer, never() ).getMetaverseBuilder();
    verify( analyzer, never() ).linkChangeNodes( any( IMetaverseNode.class ), any( IMetaverseNode.class ) );
  }

  @Test
  public void testMapChange() throws Exception {
    doReturn( outputs ).when( analyzer ).getOutputs();
    doReturn( inputs ).when( analyzer ).getInputs();

    Operation operation = mock( Operation.class );
    when( operation.getType() ).thenReturn( ChangeType.METADATA );

    StepField original = new StepField( "previousStep", "address" );
    StepField changed = new StepField( "nextStep", "address" );

    ComponentDerivationRecord cdr = new ComponentDerivationRecord( original, changed );
    cdr.addOperation( operation );
    ComponentDerivationRecord spyCdr = spy( cdr );

    analyzer.mapChange( spyCdr );
    // get operations to verify it is not null, then agains to toString it
    verify( spyCdr, times( 2 ) ).getOperations();
    verify( analyzer ).linkChangeNodes( any( IMetaverseNode.class ), any( IMetaverseNode.class ) );
  }

  @Test
  public void testMapChange_noOperation() throws Exception {
    doReturn( outputs ).when( analyzer ).getOutputs();
    doReturn( inputs ).when( analyzer ).getInputs();

    StepField original = new StepField( "previousStep", "address" );
    StepField changed = new StepField( "nextStep", "address" );

    ComponentDerivationRecord cdr = new ComponentDerivationRecord( original, changed );
    ComponentDerivationRecord spyCdr = spy( cdr );

    analyzer.mapChange( spyCdr );
    // should only be called to see if there are any operations
    verify( spyCdr ).getOperations();
    verify( analyzer ).linkChangeNodes( any( IMetaverseNode.class ), any( IMetaverseNode.class ) );
  }

  @Test
  public void testMapChange_originalFieldIsTransient() throws Exception {
    doReturn( outputs ).when( analyzer ).getOutputs();
    doReturn( inputs ).when( analyzer ).getInputs();

    IMetaverseNode transientNode = mock( IMetaverseNode.class );
    String shouldBeNull = null;
    doReturn( transientNode ).when( analyzer ).createOutputFieldNode(
      any( IAnalysisContext.class ),
      any( ValueMetaInterface.class ),
      eq( shouldBeNull ),
      eq( DictionaryConst.NODE_TYPE_TRANS_FIELD ) );

    // zip is not in the inputs or outputs, it must have been a temporary fields used internally by step.
    StepField original = new StepField( null, "zip" );
    StepField changed = new StepField( "nextStep", "address" );

    ComponentDerivationRecord cdr = new ComponentDerivationRecord( original, changed );

    analyzer.mapChange( cdr );

    verify( builder ).addLink( rootNode, DictionaryConst.LINK_TRANSIENT, transientNode );
    verify( builder ).addLink( rootNode, DictionaryConst.LINK_USES, transientNode );
    verify( analyzer ).linkChangeNodes( eq( transientNode ), any( IMetaverseNode.class ) );
  }

  @Test
  public void testMapChange_changedFieldIsTransient() throws Exception {
    doReturn( outputs ).when( analyzer ).getOutputs();
    doReturn( inputs ).when( analyzer ).getInputs();

    IMetaverseNode transientNode = mock( IMetaverseNode.class );
    String shouldBeNull = null;
    doReturn( transientNode ).when( analyzer ).createOutputFieldNode(
      any( IAnalysisContext.class ),
      any( ValueMetaInterface.class ),
      eq( shouldBeNull ),
      eq( DictionaryConst.NODE_TYPE_TRANS_FIELD ) );

    // zip is not in the inputs or outputs, it must have been a temporary fields used internally by step.
    StepField original = new StepField( "nextStep", "address" );
    StepField changed = new StepField( null, "zip" );

    ComponentDerivationRecord cdr = new ComponentDerivationRecord( original, changed );

    analyzer.mapChange( cdr );

    verify( builder ).addLink( rootNode, "transient", transientNode );
    verify( analyzer ).linkChangeNodes( any( IMetaverseNode.class ), eq( transientNode ) );
  }

  @Test
  public void testProcessOutputs_noOutputRowMetaInterfaces() throws Exception {
    doReturn( null ).when( analyzer ).getOutputRowMetaInterfaces( baseStepMeta );
    StepNodes stepNodes = analyzer.processOutputs( baseStepMeta );
    assertTrue( CollectionUtils.isEmpty( stepNodes.getStepNames() ) );
    assertTrue( CollectionUtils.isEmpty( stepNodes.getFieldNames() ) );
  }

  @Test
  public void testProcessOutputs() throws Exception {
    Map<String, RowMetaInterface> outputRmis = new HashMap<>();
    RowMetaInterface rowMetaInterface = mock( RowMetaInterface.class );
    outputRmis.put( "nextStep", rowMetaInterface );

    List<ValueMetaInterface> vmis = new ArrayList<>();
    vmis.add( new ValueMeta( "full name" ) );
    vmis.add( new ValueMeta( "address" ) );
    vmis.add( new ValueMeta( "email" ) );
    vmis.add( new ValueMeta( "date of birth" ) );
    vmis.add( new ValueMeta( "ID" ) );
    vmis.add( new ValueMeta( "occupation" ) );

    doReturn( outputRmis ).when( analyzer ).getOutputRowMetaInterfaces( baseStepMeta );
    when( rowMetaInterface.getValueMetaList() ).thenReturn( vmis );

    doReturn( fieldNode ).when( analyzer ).createOutputFieldNode(
      any( IAnalysisContext.class ),
      any( ValueMetaInterface.class ),
      eq( "nextStep" ),
      eq( DictionaryConst.NODE_TYPE_TRANS_FIELD ) );

    StepNodes stepNodes = analyzer.processOutputs( baseStepMeta );

    assertEquals( 1, stepNodes.getStepNames().size() );
    assertEquals( vmis.size(), stepNodes.getFieldNames().size() );

    for ( ValueMetaInterface vmi : vmis ) {
      // make sure we added one node for each ValueMetaInterface
      verify( analyzer ).createOutputFieldNode(
        any( IAnalysisContext.class ),
        eq( vmi ),
        eq( "nextStep" ),
        eq( DictionaryConst.NODE_TYPE_TRANS_FIELD ) );
    }

    verify( builder, times( vmis.size() ) ).addLink( rootNode, DictionaryConst.LINK_OUTPUTS, fieldNode );
  }

  @Test
  public void testGetPrevFieldDescriptor() throws Exception {
    IComponentDescriptor descriptor = analyzer.getPrevFieldDescriptor( "previousStep", "address" );
    assertNotNull( descriptor );
    assertEquals( "address" , descriptor.getName() );
    assertEquals( DictionaryConst.NODE_TYPE_TRANS_FIELD, descriptor.getType() );
    assertTrue( descriptor.getNamespace().getNamespaceId().contains( "previousStep" ) );
  }

  @Test
  public void testGetPrevFieldDescriptor_nullPrevStep() throws Exception {
    IComponentDescriptor descriptor = analyzer.getPrevFieldDescriptor( null, "address" );
    assertNull( descriptor );
  }

  @Test
  public void testCreateFieldNode() throws Exception {
    IComponentDescriptor fieldDescriptor = mock( IComponentDescriptor.class );
    ValueMetaInterface fieldMeta = new ValueMeta( "address" );

    MetaverseTransientNode node = new MetaverseTransientNode( "hello" );
    doReturn( node ).when( analyzer ).createNodeFromDescriptor( fieldDescriptor );

    IMetaverseNode fieldNode = analyzer.createFieldNode( fieldDescriptor, fieldMeta, "nextStep", true );
    assertNotNull( fieldNode );
    assertNotNull( fieldNode.getProperty( DictionaryConst.PROPERTY_KETTLE_TYPE ) );
    assertEquals( "nextStep", fieldNode.getProperty( DictionaryConst.PROPERTY_TARGET_STEP ) );

    // make sure it got added to the graph
    verify( builder ).addNode( node );
  }

  @Test
  public void testCreateFieldNode_virtual() throws Exception {
    IComponentDescriptor fieldDescriptor = mock( IComponentDescriptor.class );
    ValueMetaInterface fieldMeta = new ValueMeta( "address" );

    MetaverseTransientNode node = new MetaverseTransientNode( "hello" );
    doReturn( node ).when( analyzer ).createNodeFromDescriptor( fieldDescriptor );

    IMetaverseNode fieldNode = analyzer.createFieldNode( fieldDescriptor, fieldMeta, "nextStep", false );
    assertNotNull( fieldNode );
    assertNotNull( fieldNode.getProperty( DictionaryConst.PROPERTY_KETTLE_TYPE ) );
    assertEquals( "nextStep", fieldNode.getProperty( DictionaryConst.PROPERTY_TARGET_STEP ) );

    // make sure it did not added to the graph
    verify( builder, never() ).addNode( node );
  }

  @Test
  public void testCreateFieldNode_NoTargetStep() throws Exception {
    IComponentDescriptor fieldDescriptor = mock( IComponentDescriptor.class );
    ValueMetaInterface fieldMeta = new ValueMeta( "address" );

    MetaverseTransientNode node = new MetaverseTransientNode( "hello" );
    doReturn( node ).when( analyzer ).createNodeFromDescriptor( fieldDescriptor );

    IMetaverseNode fieldNode = analyzer.createFieldNode( fieldDescriptor, fieldMeta, null, true );
    assertNotNull( fieldNode );
    assertNotNull( fieldNode.getProperty( DictionaryConst.PROPERTY_KETTLE_TYPE ) );
    assertNull( fieldNode.getProperty( DictionaryConst.PROPERTY_TARGET_STEP ) );

    // make sure it did not added to the graph
    verify( builder, never() ).addNode( node );
  }

  @Test
  public void testCreateNodeFromDescriptor() throws Exception {
    // it just calls super, test it to get coverage
    IComponentDescriptor fieldDescriptor = mock( IComponentDescriptor.class );
    try {
      analyzer.createNodeFromDescriptor( fieldDescriptor );
    } catch ( NullPointerException npe ) {
      // expect this since the BaseKettleMetaverseComponent is actually handling this and is not set up
      // properly for testing via sub-class
    }
  }

  @Test
  public void testCreateInputFieldNode() throws Exception {
    doReturn( "thisStepName" ).when( analyzer ).getStepName();

    MetaverseTransientNode node = new MetaverseTransientNode( "node id" );
    doReturn( node ).when( analyzer ).createNodeFromDescriptor( any( IComponentDescriptor.class ) );

    IMetaverseNode inputFieldNode = analyzer.createInputFieldNode( "before", "name", 1 );
    assertNotNull( inputFieldNode );

    assertNotNull( inputFieldNode.getProperty( DictionaryConst.PROPERTY_KETTLE_TYPE ) );
    assertEquals( "thisStepName", inputFieldNode.getProperty( DictionaryConst.PROPERTY_TARGET_STEP ) );

    // the input node should not be added by this step
    verify( builder, never() ).addNode( inputFieldNode );
  }

  @Test
  public void testCreateOutputFieldNode() throws Exception {
    doReturn( "thisStepName" ).when( analyzer ).getStepName();

    MetaverseTransientNode node = new MetaverseTransientNode( "node id" );
    doReturn( node ).when( analyzer ).createNodeFromDescriptor( any( IComponentDescriptor.class ) );

    ValueMetaInterface vmi = new ValueMeta( "name", 1 );

    IMetaverseNode outputFieldNode = analyzer.createOutputFieldNode(
      descriptor.getContext(),
      vmi,
      "thisStepName",
      DictionaryConst.NODE_TYPE_TRANS_FIELD );

    assertNotNull( outputFieldNode );

    assertNotNull( outputFieldNode.getProperty( DictionaryConst.PROPERTY_KETTLE_TYPE ) );
    assertEquals( "thisStepName", outputFieldNode.getProperty( DictionaryConst.PROPERTY_TARGET_STEP ) );
    // the input node should be added by this step
    verify( builder ).addNode( outputFieldNode );
  }

  @Test
  public void testProcessInputs_noPrevSteps() throws Exception {
    when( parentTransMeta.getPrevStepNames( parentStepMeta ) ).thenReturn( null );
    StepNodes stepNodes = analyzer.processInputs( baseStepMeta );
    assertNotNull( stepNodes );
    assertEquals( 0, stepNodes.getStepNames().size() );
    assertEquals( 0, stepNodes.getFieldNames().size() );
  }

  @Test
  public void testProcessInputs() throws Exception {
    String[] prevStepNames = new String[] { "prevStep", "prevStep2" };
    when( parentTransMeta.getPrevStepNames( parentStepMeta ) ).thenReturn( prevStepNames );

    Map<String, RowMetaInterface> outputRmis = new HashMap<>();
    RowMetaInterface rowMetaInterface = mock( RowMetaInterface.class );
    RowMetaInterface rowMetaInterface2 = mock( RowMetaInterface.class );
    outputRmis.put( "prevStep", rowMetaInterface );
    outputRmis.put( "prevStep2", rowMetaInterface2 );
    String[] inputFields1 = new String[]{ "name", "address", "email", "age" };
    String[] inputFields2 = new String[]{ "employeeNumber", "occupation" };

    when( rowMetaInterface.getFieldNames() ).thenReturn( inputFields1 );
    when( rowMetaInterface2.getFieldNames() ).thenReturn( inputFields2 );

    final List<ValueMetaInterface> vmis = new ArrayList<>();
    for ( String s : inputFields1 ) {
      vmis.add( new ValueMeta( s ) );
    }

    final List<ValueMetaInterface> vmis2 = new ArrayList<>();
    for ( String s : inputFields2 ) {
      vmis2.add( new ValueMeta( s ) );
    }

    when( rowMetaInterface.getValueMeta( anyInt() ) ).thenAnswer( new Answer<ValueMetaInterface>() {
      @Override public ValueMetaInterface answer( InvocationOnMock invocationOnMock ) throws Throwable {
        Integer idx = (Integer) invocationOnMock.getArguments()[0];
        return vmis.get( idx );
      }
    } );

    when( rowMetaInterface2.getValueMeta( anyInt() ) ).thenAnswer( new Answer<ValueMetaInterface>() {
      @Override public ValueMetaInterface answer( InvocationOnMock invocationOnMock ) throws Throwable {
        Integer idx = (Integer) invocationOnMock.getArguments()[0];
        return vmis2.get( idx );
      }
    } );

    IMetaverseNode inputNode  = mock( IMetaverseNode.class );
    doReturn( outputRmis ).when( analyzer ).getInputFields( baseStepMeta );
    doReturn( inputNode ).when( analyzer ).createInputFieldNode( anyString(), anyString(), anyInt() );

    StepNodes stepNodes = analyzer.processInputs( baseStepMeta );
    assertNotNull( stepNodes );
    verify( builder, times( 6 ) ).addLink(
      any( IMetaverseNode.class ),
      eq( DictionaryConst.LINK_INPUTS ),
      eq( rootNode ) );
  }

  @Test
  public void testSimpleGetters() throws Exception {
    assertNull( analyzer.getInputs() );
    assertNull( analyzer.getOutputs() );
    assertEquals( parentStepMeta.getName(),  analyzer.getStepName() );
  }

  @Test
  public void testCreateStepFields() throws Exception {
    StepNodes stepNodes = new StepNodes();
    IMetaverseNode node = mock( IMetaverseNode.class );
    stepNodes.addNode( "step1", "fieldname1", node );
    stepNodes.addNode( "step2", "fieldname2", node );

    Set<StepField> test = analyzer.createStepFields( "test", stepNodes );
    assertEquals( 2, test.size() );
    for ( StepField stepField : test ) {
      assertEquals( "test", stepField.getFieldName() );
    }
  }

  @Test
  public void testGetOutputRowMetaInterfaces() throws Exception {
    String[] nextStepNames = new String[] { "nextStep1" };
    when( parentTransMeta.getNextStepNames( parentStepMeta ) ).thenReturn( nextStepNames );

    RowMetaInterface rowMetaInterface = mock( RowMetaInterface.class );
    doReturn( rowMetaInterface ).when( analyzer ).getOutputFields( baseStepMeta );

    Map<String, RowMetaInterface> rowMetaInterfaces = analyzer.getOutputRowMetaInterfaces( baseStepMeta );
    assertNotNull( rowMetaInterfaces );
    assertEquals( nextStepNames.length, rowMetaInterfaces.size() );
    assertEquals( rowMetaInterface, rowMetaInterfaces.get( nextStepNames[ 0 ] ) );
  }

  @Test
  public void testGetOutputRowMetaInterfaces_multipleOutputSteps() throws Exception {
    String[] nextStepNames = new String[] { "nextStep1", "nextStep2" };
    when( parentTransMeta.getNextStepNames( parentStepMeta ) ).thenReturn( nextStepNames );

    RowMetaInterface rowMetaInterface = mock( RowMetaInterface.class );
    doReturn( rowMetaInterface ).when( analyzer ).getOutputFields( baseStepMeta );

    Map<String, RowMetaInterface> rowMetaInterfaces = analyzer.getOutputRowMetaInterfaces( baseStepMeta );
    assertNotNull( rowMetaInterfaces );
    assertEquals( nextStepNames.length, rowMetaInterfaces.size() );
    assertEquals( rowMetaInterface, rowMetaInterfaces.get( nextStepNames[ 0 ] ) );
    assertEquals( rowMetaInterface, rowMetaInterfaces.get( nextStepNames[ 1 ] ) );
  }

  @Test
  public void testGetOutputRowMetaInterfaces_noNextSteps() throws Exception {
    when( parentTransMeta.getNextStepNames( parentStepMeta ) ).thenReturn( null );

    RowMetaInterface rowMetaInterface = mock( RowMetaInterface.class );
    doReturn( rowMetaInterface ).when( analyzer ).getOutputFields( baseStepMeta );

    Map<String, RowMetaInterface> rowMetaInterfaces = analyzer.getOutputRowMetaInterfaces( baseStepMeta );
    assertNotNull( rowMetaInterfaces );
    assertEquals( 1, rowMetaInterfaces.size() );
    assertEquals( rowMetaInterface, rowMetaInterfaces.get( StepAnalyzer.NONE ) );
  }

  @Test
  public void testAnalyze() throws Exception {

    when( baseStepMeta.getParentStepMeta() ).thenReturn( parentStepMeta );
    when( parentStepMeta.getCopies() ).thenReturn( 2 );
    analyzer.setMetaverseBuilder( builder );

    doNothing().when( analyzer ).validateState( descriptor, baseStepMeta );
    doNothing().when( analyzer ).addConnectionNodes( descriptor );
    doReturn( null ).when( analyzer ).processInputs( baseStepMeta );
    doReturn( null ).when( analyzer ).processOutputs( baseStepMeta );

    Set<StepField> usedFields = new HashSet<>();
    usedFields.add( new StepField( "inputStep", "name" ) );
    doReturn( usedFields ).when( analyzer ).getUsedFields( baseStepMeta );

    doNothing().when( analyzer ).processUsedFields( usedFields );

    Set<ComponentDerivationRecord> changeRecords = new HashSet<>();
    changeRecords.add( mock( ComponentDerivationRecord.class ) );
    changeRecords.add( mock( ComponentDerivationRecord.class ) );

    doReturn( changeRecords ).when( analyzer ).getChanges();
    doNothing().when( analyzer ).mapChange( any( ComponentDerivationRecord.class ) );

    MetaverseTransientNode node = new MetaverseTransientNode( "hello" );
    doNothing().when( analyzer ).customAnalyze( baseStepMeta, node );

    when( parentStepMeta.getStepID() ).thenReturn( "step id" );
    doReturn( node ).when( analyzer ).createNodeFromDescriptor( descriptor );

    IMetaverseNode analyzedNode = analyzer.analyze( descriptor, baseStepMeta );

    assertNotNull( analyzedNode.getProperty( "stepType" ) );
    assertEquals( 2, analyzedNode.getProperty( "copies" ) );
    assertEquals( analyzer.getClass().getSimpleName(), analyzedNode.getProperty( "_analyzer" ) );

    verify( builder ).addNode( analyzedNode );
    verify( analyzer ).addConnectionNodes( descriptor );
    verify( analyzer ).processInputs( baseStepMeta );
    verify( analyzer ).processOutputs( baseStepMeta );
    verify( analyzer ).getUsedFields( baseStepMeta );
    verify( analyzer ).processUsedFields( usedFields );
    verify( analyzer ).getChanges();
    verify( analyzer, times( changeRecords.size() ) ).mapChange( any( ComponentDerivationRecord.class ) );
    verify( analyzer ).customAnalyze( baseStepMeta, analyzedNode );
  }
}
