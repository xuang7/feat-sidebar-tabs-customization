/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package edu.uci.ics.amber.operator

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation._
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import edu.uci.ics.amber.core.executor.OperatorExecutor
import edu.uci.ics.amber.core.tuple.Schema
import edu.uci.ics.amber.core.virtualidentity.{
  ExecutionIdentity,
  OperatorIdentity,
  WorkflowIdentity
}
import edu.uci.ics.amber.core.workflow.WorkflowContext.{DEFAULT_EXECUTION_ID, DEFAULT_WORKFLOW_ID}
import edu.uci.ics.amber.core.workflow.{PhysicalOp, PhysicalPlan, PortIdentity}
import edu.uci.ics.amber.operator.aggregate.AggregateOpDesc
import edu.uci.ics.amber.operator.cartesianProduct.CartesianProductOpDesc
import edu.uci.ics.amber.operator.dictionary.DictionaryMatcherOpDesc
import edu.uci.ics.amber.operator.difference.DifferenceOpDesc
import edu.uci.ics.amber.operator.distinct.DistinctOpDesc
import edu.uci.ics.amber.operator.dummy.DummyOpDesc
import edu.uci.ics.amber.operator.filter.SpecializedFilterOpDesc
import edu.uci.ics.amber.operator.hashJoin.HashJoinOpDesc
import edu.uci.ics.amber.operator.huggingFace.{
  HuggingFaceIrisLogisticRegressionOpDesc,
  HuggingFaceSentimentAnalysisOpDesc,
  HuggingFaceSpamSMSDetectionOpDesc,
  HuggingFaceTextSummarizationOpDesc
}
import edu.uci.ics.amber.operator.ifStatement.IfOpDesc
import edu.uci.ics.amber.operator.intersect.IntersectOpDesc
import edu.uci.ics.amber.operator.intervalJoin.IntervalJoinOpDesc
import edu.uci.ics.amber.operator.keywordSearch.KeywordSearchOpDesc
import edu.uci.ics.amber.operator.limit.LimitOpDesc
import edu.uci.ics.amber.operator.machineLearning.Scorer.MachineLearningScorerOpDesc
import edu.uci.ics.amber.operator.machineLearning.sklearnAdvanced.KNNTrainer.{
  SklearnAdvancedKNNClassifierTrainerOpDesc,
  SklearnAdvancedKNNRegressorTrainerOpDesc
}
import edu.uci.ics.amber.operator.machineLearning.sklearnAdvanced.SVCTrainer.SklearnAdvancedSVCTrainerOpDesc
import edu.uci.ics.amber.operator.machineLearning.sklearnAdvanced.SVRTrainer.SklearnAdvancedSVRTrainerOpDesc
import edu.uci.ics.amber.operator.metadata.{OPVersion, OperatorInfo, PropertyNameConstants}
import edu.uci.ics.amber.operator.projection.ProjectionOpDesc
import edu.uci.ics.amber.operator.randomksampling.RandomKSamplingOpDesc
import edu.uci.ics.amber.operator.regex.RegexOpDesc
import edu.uci.ics.amber.operator.reservoirsampling.ReservoirSamplingOpDesc
import edu.uci.ics.amber.operator.sklearn._
import edu.uci.ics.amber.operator.sleep.SleepOpDesc
import edu.uci.ics.amber.operator.sklearn.training.{
  SklearnTrainingAdaptiveBoostingOpDesc,
  SklearnTrainingBaggingOpDesc,
  SklearnTrainingBernoulliNaiveBayesOpDesc,
  SklearnTrainingComplementNaiveBayesOpDesc,
  SklearnTrainingDecisionTreeOpDesc,
  SklearnTrainingDummyClassifierOpDesc,
  SklearnTrainingExtraTreeOpDesc,
  SklearnTrainingExtraTreesOpDesc,
  SklearnTrainingGaussianNaiveBayesOpDesc,
  SklearnTrainingGradientBoostingOpDesc,
  SklearnTrainingKNNOpDesc,
  SklearnTrainingLinearSVMOpDesc,
  SklearnTrainingMultiLayerPerceptronOpDesc,
  SklearnTrainingMultinomialNaiveBayesOpDesc,
  SklearnTrainingNearestCentroidOpDesc,
  SklearnTrainingPassiveAggressiveOpDesc,
  SklearnTrainingPerceptronOpDesc,
  SklearnTrainingProbabilityCalibrationOpDesc,
  SklearnTrainingRandomForestOpDesc,
  SklearnTrainingRidgeCVOpDesc,
  SklearnTrainingRidgeOpDesc,
  SklearnTrainingSDGOpDesc,
  SklearnTrainingSVMOpDesc
}
import edu.uci.ics.amber.operator.sort.SortOpDesc
import edu.uci.ics.amber.operator.sortPartitions.SortPartitionsOpDesc
import edu.uci.ics.amber.operator.source.apis.reddit.RedditSearchSourceOpDesc
import edu.uci.ics.amber.operator.source.apis.twitter.v2.{
  TwitterFullArchiveSearchSourceOpDesc,
  TwitterSearchSourceOpDesc
}
import edu.uci.ics.amber.operator.source.fetcher.URLFetcherOpDesc
import edu.uci.ics.amber.operator.source.scan.FileScanSourceOpDesc
import edu.uci.ics.amber.operator.source.scan.arrow.ArrowSourceOpDesc
import edu.uci.ics.amber.operator.source.scan.csv.CSVScanSourceOpDesc
import edu.uci.ics.amber.operator.source.scan.csvOld.CSVOldScanSourceOpDesc
import edu.uci.ics.amber.operator.source.scan.json.JSONLScanSourceOpDesc
import edu.uci.ics.amber.operator.source.scan.text.TextInputSourceOpDesc
import edu.uci.ics.amber.operator.source.sql.asterixdb.AsterixDBSourceOpDesc
import edu.uci.ics.amber.operator.source.sql.mysql.MySQLSourceOpDesc
import edu.uci.ics.amber.operator.source.sql.postgresql.PostgreSQLSourceOpDesc
import edu.uci.ics.amber.operator.split.SplitOpDesc
import edu.uci.ics.amber.operator.symmetricDifference.SymmetricDifferenceOpDesc
import edu.uci.ics.amber.operator.typecasting.TypeCastingOpDesc
import edu.uci.ics.amber.operator.udf.java.JavaUDFOpDesc
import edu.uci.ics.amber.operator.udf.python._
import edu.uci.ics.amber.operator.udf.python.source.PythonUDFSourceOpDescV2
import edu.uci.ics.amber.operator.udf.r.{RUDFOpDesc, RUDFSourceOpDesc}
import edu.uci.ics.amber.operator.union.UnionOpDesc
import edu.uci.ics.amber.operator.unneststring.UnnestStringOpDesc
import edu.uci.ics.amber.operator.visualization.DotPlot.DotPlotOpDesc
import edu.uci.ics.amber.operator.visualization.IcicleChart.IcicleChartOpDesc
import edu.uci.ics.amber.operator.visualization.ImageViz.ImageVisualizerOpDesc
import edu.uci.ics.amber.operator.visualization.ScatterMatrixChart.ScatterMatrixChartOpDesc
import edu.uci.ics.amber.operator.visualization.barChart.BarChartOpDesc
import edu.uci.ics.amber.operator.visualization.boxViolinPlot.BoxViolinPlotOpDesc
import edu.uci.ics.amber.operator.visualization.bubbleChart.BubbleChartOpDesc
import edu.uci.ics.amber.operator.visualization.bulletChart.BulletChartOpDesc
import edu.uci.ics.amber.operator.visualization.candlestickChart.CandlestickChartOpDesc
import edu.uci.ics.amber.operator.visualization.continuousErrorBands.ContinuousErrorBandsOpDesc
import edu.uci.ics.amber.operator.visualization.contourPlot.ContourPlotOpDesc
import edu.uci.ics.amber.operator.visualization.dumbbellPlot.DumbbellPlotOpDesc
import edu.uci.ics.amber.operator.visualization.figureFactoryTable.FigureFactoryTableOpDesc
import edu.uci.ics.amber.operator.visualization.filledAreaPlot.FilledAreaPlotOpDesc
import edu.uci.ics.amber.operator.visualization.funnelPlot.FunnelPlotOpDesc
import edu.uci.ics.amber.operator.visualization.ganttChart.GanttChartOpDesc
import edu.uci.ics.amber.operator.visualization.gaugeChart.GaugeChartOpDesc
import edu.uci.ics.amber.operator.visualization.timeSeriesplot.TimeSeriesOpDesc
import edu.uci.ics.amber.operator.visualization.heatMap.HeatMapOpDesc
import edu.uci.ics.amber.operator.visualization.hierarchychart.HierarchyChartOpDesc
import edu.uci.ics.amber.operator.visualization.histogram.HistogramChartOpDesc
import edu.uci.ics.amber.operator.visualization.htmlviz.HtmlVizOpDesc
import edu.uci.ics.amber.operator.visualization.lineChart.LineChartOpDesc
import edu.uci.ics.amber.operator.visualization.networkGraph.NetworkGraphOpDesc
import edu.uci.ics.amber.operator.visualization.pieChart.PieChartOpDesc
import edu.uci.ics.amber.operator.visualization.quiverPlot.QuiverPlotOpDesc
import edu.uci.ics.amber.operator.visualization.rangeSlider.RangeSliderOpDesc
import edu.uci.ics.amber.operator.visualization.sankeyDiagram.SankeyDiagramOpDesc
import edu.uci.ics.amber.operator.visualization.scatter3DChart.Scatter3dChartOpDesc
import edu.uci.ics.amber.operator.visualization.scatterplot.ScatterplotOpDesc
import edu.uci.ics.amber.operator.visualization.tablesChart.TablesPlotOpDesc
import edu.uci.ics.amber.operator.visualization.ternaryPlot.TernaryPlotOpDesc
import edu.uci.ics.amber.operator.visualization.urlviz.UrlVizOpDesc
import edu.uci.ics.amber.operator.visualization.volcanoPlot.VolcanoPlotOpDesc
import edu.uci.ics.amber.operator.visualization.waterfallChart.WaterfallChartOpDesc
import edu.uci.ics.amber.operator.visualization.wordCloud.WordCloudOpDesc
import edu.uci.ics.amber.operator.visualization.dendrogram.DendrogramOpDesc
import edu.uci.ics.amber.operator.visualization.histogram2d.Histogram2DOpDesc
import edu.uci.ics.amber.operator.visualization.nestedTable.NestedTableOpDesc
import edu.uci.ics.amber.operator.visualization.treeplot.TreePlotOpDesc
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder, ToStringBuilder}

import java.util.UUID
import scala.util.Try

trait StateTransferFunc
    extends ((OperatorExecutor, OperatorExecutor) => Unit)
    with java.io.Serializable

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "operatorType"
)
@JsonSubTypes(
  Array(
    new Type(value = classOf[IfOpDesc], name = "If"),
    new Type(value = classOf[SankeyDiagramOpDesc], name = "SankeyDiagram"),
    new Type(value = classOf[IcicleChartOpDesc], name = "IcicleChart"),
    new Type(value = classOf[CSVScanSourceOpDesc], name = "CSVFileScan"),
    // disabled the ParallelCSVScanSourceOpDesc so that it does not confuse user. it can be re-enabled when doing experiments.
    // new Type(value = classOf[ParallelCSVScanSourceOpDesc], name = "ParallelCSVFileScan"),
    new Type(value = classOf[JSONLScanSourceOpDesc], name = "JSONLFileScan"),
    new Type(value = classOf[FileScanSourceOpDesc], name = "FileScan"),
    new Type(value = classOf[TextInputSourceOpDesc], name = "TextInput"),
    new Type(
      value = classOf[TwitterFullArchiveSearchSourceOpDesc],
      name = "TwitterFullArchiveSearch"
    ),
    new Type(
      value = classOf[TwitterSearchSourceOpDesc],
      name = "TwitterSearch"
    ),
    new Type(value = classOf[TimeSeriesOpDesc], name = "TimeSeriesPlot"),
    new Type(value = classOf[CandlestickChartOpDesc], name = "CandlestickChart"),
    new Type(value = classOf[SplitOpDesc], name = "Split"),
    new Type(value = classOf[ContourPlotOpDesc], name = "ContourPlot"),
    new Type(value = classOf[RegexOpDesc], name = "Regex"),
    new Type(value = classOf[SpecializedFilterOpDesc], name = "Filter"),
    new Type(value = classOf[ProjectionOpDesc], name = "Projection"),
    new Type(value = classOf[UnionOpDesc], name = "Union"),
    new Type(value = classOf[KeywordSearchOpDesc], name = "KeywordSearch"),
    new Type(value = classOf[AggregateOpDesc], name = "Aggregate"),
    new Type(value = classOf[LineChartOpDesc], name = "LineChart"),
    new Type(value = classOf[WaterfallChartOpDesc], name = "WaterfallChart"),
    new Type(value = classOf[BarChartOpDesc], name = "BarChart"),
    new Type(value = classOf[RangeSliderOpDesc], name = "RangeSlider"),
    new Type(value = classOf[PieChartOpDesc], name = "PieChart"),
    new Type(value = classOf[QuiverPlotOpDesc], name = "QuiverPlot"),
    new Type(value = classOf[WordCloudOpDesc], name = "WordCloud"),
    new Type(value = classOf[HtmlVizOpDesc], name = "HTMLVisualizer"),
    new Type(value = classOf[UrlVizOpDesc], name = "URLVisualizer"),
    new Type(value = classOf[ScatterplotOpDesc], name = "Scatterplot"),
    new Type(value = classOf[PythonUDFOpDescV2], name = "PythonUDFV2"),
    new Type(value = classOf[PythonUDFSourceOpDescV2], name = "PythonUDFSourceV2"),
    new Type(value = classOf[DualInputPortsPythonUDFOpDescV2], name = "DualInputPortsPythonUDFV2"),
    new Type(value = classOf[MySQLSourceOpDesc], name = "MySQLSource"),
    new Type(value = classOf[PostgreSQLSourceOpDesc], name = "PostgreSQLSource"),
    new Type(value = classOf[AsterixDBSourceOpDesc], name = "AsterixDBSource"),
    new Type(value = classOf[TypeCastingOpDesc], name = "TypeCasting"),
    new Type(value = classOf[LimitOpDesc], name = "Limit"),
    new Type(value = classOf[SleepOpDesc], name = "Sleep"),
    new Type(value = classOf[RandomKSamplingOpDesc], name = "RandomKSampling"),
    new Type(value = classOf[ReservoirSamplingOpDesc], name = "ReservoirSampling"),
    new Type(value = classOf[HashJoinOpDesc[String]], name = "HashJoin"),
    new Type(value = classOf[DistinctOpDesc], name = "Distinct"),
    new Type(value = classOf[IntersectOpDesc], name = "Intersect"),
    new Type(value = classOf[SymmetricDifferenceOpDesc], name = "SymmetricDifference"),
    new Type(value = classOf[DifferenceOpDesc], name = "Difference"),
    new Type(value = classOf[IntervalJoinOpDesc], name = "IntervalJoin"),
    new Type(value = classOf[UnnestStringOpDesc], name = "UnnestString"),
    new Type(value = classOf[DictionaryMatcherOpDesc], name = "DictionaryMatcher"),
    new Type(value = classOf[SortPartitionsOpDesc], name = "SortPartitions"),
    new Type(value = classOf[CSVOldScanSourceOpDesc], name = "CSVOldFileScan"),
    new Type(value = classOf[RedditSearchSourceOpDesc], name = "RedditSearch"),
    new Type(value = classOf[PythonLambdaFunctionOpDesc], name = "PythonLambdaFunction"),
    new Type(value = classOf[PythonTableReducerOpDesc], name = "PythonTableReducer"),
    new Type(value = classOf[URLFetcherOpDesc], name = "URLFetcher"),
    new Type(value = classOf[VolcanoPlotOpDesc], name = "VolcanoPlot"),
    new Type(value = classOf[CartesianProductOpDesc], name = "CartesianProduct"),
    new Type(value = classOf[FilledAreaPlotOpDesc], name = "FilledAreaPlot"),
    new Type(value = classOf[DotPlotOpDesc], name = "DotPlot"),
    new Type(value = classOf[TreePlotOpDesc], name = "TreePlot"),
    new Type(value = classOf[BubbleChartOpDesc], name = "BubbleChart"),
    new Type(value = classOf[BulletChartOpDesc], name = "BulletChart"),
    new Type(value = classOf[GanttChartOpDesc], name = "GanttChart"),
    new Type(value = classOf[GaugeChartOpDesc], name = "GaugeChart"),
    new Type(value = classOf[ImageVisualizerOpDesc], name = "ImageVisualizer"),
    new Type(value = classOf[HierarchyChartOpDesc], name = "HierarchyChart"),
    new Type(value = classOf[DumbbellPlotOpDesc], name = "DumbbellPlot"),
    new Type(value = classOf[DummyOpDesc], name = "Dummy"),
    new Type(value = classOf[BoxViolinPlotOpDesc], name = "BoxViolinPlot"),
    new Type(value = classOf[NetworkGraphOpDesc], name = "NetworkGraph"),
    new Type(value = classOf[HistogramChartOpDesc], name = "Histogram"),
    new Type(value = classOf[Histogram2DOpDesc], name = "Histogram2D"),
    new Type(value = classOf[ScatterMatrixChartOpDesc], name = "ScatterMatrixChart"),
    new Type(value = classOf[HeatMapOpDesc], name = "HeatMap"),
    new Type(value = classOf[Scatter3dChartOpDesc], name = "Scatter3DChart"),
    new Type(value = classOf[FunnelPlotOpDesc], name = "FunnelPlot"),
    new Type(value = classOf[TablesPlotOpDesc], name = "TablesPlot"),
    new Type(value = classOf[ContinuousErrorBandsOpDesc], name = "ContinuousErrorBands"),
    new Type(value = classOf[FigureFactoryTableOpDesc], name = "FigureFactoryTable"),
    new Type(value = classOf[TernaryPlotOpDesc], name = "TernaryPlot"),
    new Type(value = classOf[DendrogramOpDesc], name = "Dendrogram"),
    new Type(value = classOf[NestedTableOpDesc], name = "NestedTable"),
    new Type(value = classOf[JavaUDFOpDesc], name = "JavaUDF"),
    new Type(value = classOf[RUDFOpDesc], name = "RUDF"),
    new Type(value = classOf[RUDFSourceOpDesc], name = "RUDFSource"),
    new Type(value = classOf[ArrowSourceOpDesc], name = "ArrowSource"),
    new Type(value = classOf[MachineLearningScorerOpDesc], name = "Scorer"),
    new Type(value = classOf[SortOpDesc], name = "Sort"),
    new Type(value = classOf[SklearnLogisticRegressionOpDesc], name = "SklearnLogisticRegression"),
    new Type(
      value = classOf[SklearnLogisticRegressionCVOpDesc],
      name = "SklearnLogisticRegressionCV"
    ),
    new Type(value = classOf[SklearnTrainingRidgeOpDesc], name = "SklearnRidge"),
    new Type(value = classOf[SklearnTrainingRidgeCVOpDesc], name = "SklearnRidgeCV"),
    new Type(value = classOf[SklearnTrainingSDGOpDesc], name = "SklearnSDG"),
    new Type(
      value = classOf[SklearnTrainingPassiveAggressiveOpDesc],
      name = "SklearnPassiveAggressive"
    ),
    new Type(value = classOf[SklearnTrainingPerceptronOpDesc], name = "SklearnPerceptron"),
    new Type(value = classOf[SklearnTrainingKNNOpDesc], name = "SklearnKNN"),
    new Type(
      value = classOf[SklearnTrainingNearestCentroidOpDesc],
      name = "SklearnNearestCentroid"
    ),
    new Type(value = classOf[SklearnTrainingSVMOpDesc], name = "SklearnSVM"),
    new Type(value = classOf[SklearnTrainingLinearSVMOpDesc], name = "SklearnLinearSVM"),
    new Type(value = classOf[SklearnTrainingDecisionTreeOpDesc], name = "SklearnDecisionTree"),
    new Type(value = classOf[SklearnTrainingExtraTreeOpDesc], name = "SklearnExtraTree"),
    new Type(
      value = classOf[SklearnTrainingMultiLayerPerceptronOpDesc],
      name = "SklearnMultiLayerPerceptron"
    ),
    new Type(
      value = classOf[SklearnTrainingProbabilityCalibrationOpDesc],
      name = "SklearnProbabilityCalibration"
    ),
    new Type(value = classOf[SklearnTrainingRandomForestOpDesc], name = "SklearnRandomForest"),
    new Type(value = classOf[SklearnTrainingBaggingOpDesc], name = "SklearnBagging"),
    new Type(
      value = classOf[SklearnTrainingGradientBoostingOpDesc],
      name = "SklearnGradientBoosting"
    ),
    new Type(
      value = classOf[SklearnTrainingAdaptiveBoostingOpDesc],
      name = "SklearnAdaptiveBoosting"
    ),
    new Type(value = classOf[SklearnTrainingExtraTreesOpDesc], name = "SklearnExtraTrees"),
    new Type(
      value = classOf[SklearnTrainingGaussianNaiveBayesOpDesc],
      name = "SklearnGaussianNaiveBayes"
    ),
    new Type(
      value = classOf[SklearnTrainingMultinomialNaiveBayesOpDesc],
      name = "SklearnMultinomialNaiveBayes"
    ),
    new Type(
      value = classOf[SklearnTrainingComplementNaiveBayesOpDesc],
      name = "SklearnComplementNaiveBayes"
    ),
    new Type(
      value = classOf[SklearnTrainingBernoulliNaiveBayesOpDesc],
      name = "SklearnBernoulliNaiveBayes"
    ),
    new Type(
      value = classOf[SklearnTrainingDummyClassifierOpDesc],
      name = "SklearnDummyClassifier"
    ),
    new Type(value = classOf[SklearnLogisticRegressionOpDesc], name = "SklearnLogisticRegression"),
    new Type(
      value = classOf[SklearnLogisticRegressionCVOpDesc],
      name = "SklearnLogisticRegressionCV"
    ),
    new Type(value = classOf[SklearnRidgeOpDesc], name = "SklearnRidge"),
    new Type(value = classOf[SklearnRidgeCVOpDesc], name = "SklearnRidgeCV"),
    new Type(value = classOf[SklearnSDGOpDesc], name = "SklearnSDG"),
    new Type(value = classOf[SklearnPassiveAggressiveOpDesc], name = "SklearnPassiveAggressive"),
    new Type(value = classOf[SklearnPerceptronOpDesc], name = "SklearnPerceptron"),
    new Type(value = classOf[SklearnKNNOpDesc], name = "SklearnKNN"),
    new Type(value = classOf[SklearnNearestCentroidOpDesc], name = "SklearnNearestCentroid"),
    new Type(value = classOf[SklearnSVMOpDesc], name = "SklearnSVM"),
    new Type(value = classOf[SklearnLinearSVMOpDesc], name = "SklearnLinearSVM"),
    new Type(value = classOf[SklearnLinearRegressionOpDesc], name = "SklearnLinearRegression"),
    new Type(value = classOf[SklearnDecisionTreeOpDesc], name = "SklearnDecisionTree"),
    new Type(value = classOf[SklearnExtraTreeOpDesc], name = "SklearnExtraTree"),
    new Type(
      value = classOf[SklearnMultiLayerPerceptronOpDesc],
      name = "SklearnMultiLayerPerceptron"
    ),
    new Type(
      value = classOf[SklearnProbabilityCalibrationOpDesc],
      name = "SklearnProbabilityCalibration"
    ),
    new Type(value = classOf[SklearnRandomForestOpDesc], name = "SklearnRandomForest"),
    new Type(value = classOf[SklearnBaggingOpDesc], name = "SklearnBagging"),
    new Type(value = classOf[SklearnGradientBoostingOpDesc], name = "SklearnGradientBoosting"),
    new Type(value = classOf[SklearnAdaptiveBoostingOpDesc], name = "SklearnAdaptiveBoosting"),
    new Type(value = classOf[SklearnExtraTreesOpDesc], name = "SklearnExtraTrees"),
    new Type(value = classOf[SklearnGaussianNaiveBayesOpDesc], name = "SklearnGaussianNaiveBayes"),
    new Type(
      value = classOf[SklearnMultinomialNaiveBayesOpDesc],
      name = "SklearnMultinomialNaiveBayes"
    ),
    new Type(
      value = classOf[SklearnComplementNaiveBayesOpDesc],
      name = "SklearnComplementNaiveBayes"
    ),
    new Type(
      value = classOf[SklearnBernoulliNaiveBayesOpDesc],
      name = "SklearnBernoulliNaiveBayes"
    ),
    new Type(value = classOf[SklearnDummyClassifierOpDesc], name = "SklearnDummyClassifier"),
    new Type(value = classOf[SklearnPredictionOpDesc], name = "SklearnPrediction"),
    new Type(
      value = classOf[HuggingFaceSentimentAnalysisOpDesc],
      name = "HuggingFaceSentimentAnalysis"
    ),
    new Type(
      value = classOf[HuggingFaceTextSummarizationOpDesc],
      name = "HuggingFaceTextSummarization"
    ),
    new Type(
      value = classOf[HuggingFaceSpamSMSDetectionOpDesc],
      name = "HuggingFaceSpamSMSDetection"
    ),
    new Type(
      value = classOf[HuggingFaceIrisLogisticRegressionOpDesc],
      name = "HuggingFaceIrisLogisticRegression"
    ),
    new Type(
      value = classOf[SklearnAdvancedKNNClassifierTrainerOpDesc],
      name = "KNNClassifierTrainer"
    ),
    new Type(
      value = classOf[SklearnAdvancedKNNRegressorTrainerOpDesc],
      name = "KNNRegressorTrainer"
    ),
    new Type(
      value = classOf[SklearnAdvancedSVCTrainerOpDesc],
      name = "SVCTrainer"
    ),
    new Type(
      value = classOf[SklearnAdvancedSVRTrainerOpDesc],
      name = "SVRTrainer"
    )
  )
)
abstract class LogicalOp extends PortDescriptor with Serializable {

  @JsonProperty(PropertyNameConstants.OPERATOR_ID)
  private var operatorId: String = getClass.getSimpleName + "-" + UUID.randomUUID.toString

  @JsonProperty(PropertyNameConstants.OPERATOR_VERSION)
  var operatorVersion: String = getOperatorVersion

  def operatorIdentifier: OperatorIdentity = OperatorIdentity(operatorId)

  def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = ???

  // a logical operator corresponds multiple physical operators (a small DAG)
  def getPhysicalPlan(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalPlan = {
    PhysicalPlan(
      operators = Set(getPhysicalOp(workflowId, executionId)),
      links = Set.empty
    )
  }

  def operatorInfo: OperatorInfo

  private def getOperatorVersion: String = {
    val path = "core/amber/src/main/scala/"
    val operatorPath = path + this.getClass.getPackage.getName.replace(".", "/")
    OPVersion.getVersion(this.getClass.getSimpleName, operatorPath)
  }

  override def hashCode: Int = HashCodeBuilder.reflectionHashCode(this)

  override def equals(that: Any): Boolean = EqualsBuilder.reflectionEquals(this, that, "context")

  override def toString: String = ToStringBuilder.reflectionToString(this)

  def setOperatorId(id: String): Unit = {
    operatorId = id
  }

  def runtimeReconfiguration(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity,
      oldOpDesc: LogicalOp,
      newOpDesc: LogicalOp
  ): Try[(PhysicalOp, Option[StateTransferFunc])] = {
    throw new UnsupportedOperationException(
      "operator " + getClass.getSimpleName + " does not support reconfiguration"
    )
  }

  @JsonProperty
  @JsonSchemaTitle("Dummy Property List")
  @JsonPropertyDescription("Add dummy property if needed")
  var dummyPropertyList: List[DummyProperties] = List()

  /**
    * Propagates the schema from external input ports to external output ports.
    * This method is primarily used to derive the output schemas for logical operators.
    *
    * @param inputSchemas A map containing the schemas of the external input ports.
    * @return A map of external output port identities to their corresponding schemas.
    */
  def getExternalOutputSchemas(
      inputSchemas: Map[PortIdentity, Schema]
  ): Map[PortIdentity, Schema] = {
    this
      .getPhysicalPlan(DEFAULT_WORKFLOW_ID, DEFAULT_EXECUTION_ID)
      .propagateSchema(inputSchemas)
      .operators
      .flatMap { operator =>
        operator.outputPorts.values
          .filterNot { case (port, _, _) => port.id.internal } // Exclude internal ports
          .map {
            case (port, _, schemaEither) =>
              schemaEither match {
                case Left(error) => throw error
                case Right(schema) =>
                  port.id -> schema // Map external port ID to its schema
              }
          }
      }
      .toMap
  }
}
