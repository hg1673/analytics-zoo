/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.zoo.pipeline.common.nn

import breeze.linalg.{DenseMatrix, convert}
import breeze.numerics._
import com.intel.analytics.bigdl.nn.Module
import com.intel.analytics.bigdl.tensor.{Storage, Tensor}
import com.intel.analytics.bigdl.utils.T
import com.intel.analytics.zoo.pipeline.common.BboxUtil
import org.scalatest.{FlatSpec, Matchers}

class ProposalTargetSpec extends FlatSpec with Matchers {

  val exRois = DenseMatrix((0.543404941791, 0.278369385094, 0.424517590749, 0.84477613232),
    (0.00471885619097, 0.121569120783, 0.670749084727, 0.825852755105),
    (0.136706589685, 0.575093329427, 0.891321954312, 0.209202122117),
    (0.18532821955, 0.108376890464, 0.219697492625, 0.978623784707),
    (0.811683149089, 0.171941012733, 0.816224748726, 0.274073747042),
    (0.431704183663, 0.940029819622, 0.817649378777, 0.336111950121),
    (0.175410453742, 0.37283204629, 0.00568850735257, 0.252426353445),
    (0.795662508473, 0.0152549712463, 0.598843376928, 0.603804539043),
    (0.105147685412, 0.381943444943, 0.0364760565926, 0.890411563442),
    (0.980920857012, 0.059941988818, 0.890545944729, 0.5769014994))
  val gtRois = DenseMatrix((0.742479689098, 0.630183936475, 0.581842192399, 0.0204391320269),
    (0.210026577673, 0.544684878179, 0.769115171106, 0.250695229138),
    (0.285895690407, 0.852395087841, 0.975006493607, 0.884853293491),
    (0.359507843937, 0.598858945876, 0.354795611657, 0.340190215371),
    (0.178080989506, 0.237694208624, 0.0448622824608, 0.505431429636),
    (0.376252454297, 0.592805400976, 0.629941875587, 0.142600314446),
    (0.933841299466, 0.946379880809, 0.602296657731, 0.387766280327),
    (0.363188004109, 0.204345276869, 0.276765061396, 0.246535881204),
    (0.17360800174, 0.966609694487, 0.957012600353, 0.597973684329),
    (0.73130075306, 0.340385222837, 0.0920556033772, 0.463498018937)
  )


  val labels = Array(0.508698893238, 0.0884601730029, 0.528035223318, 0.992158036511,
    0.395035931758, 0.335596441719, 0.805450537329, 0.754348994582, 0.313066441589,
    0.634036682962).map(x => x.toFloat)


  behavior of "ProposalTargetSpec"

  val proposalTarget = new ProposalTarget(128, 21)
  it should "computeTargets without norm correcly" in {
    val expected = DenseMatrix((0.508699, 0.202244, -0.15083, -0.0485428, -1.38974),
      (0.0884602, 0.0911369, -0.0446058, -0.0663423, -0.88127),
      (0.528035, 0.0663603, 0.751411, -0.0380474, 0.487477),
      (0.992158, 0.149501, -0.039554, -0.0385152, -0.925378),
      (0.395036, -0.699306, 0.134789, -0.1475, 0.139986),
      (0.335596, -0.0877232, -0.682606, -0.100292, 0.327924),
      (0.805451, 0.816015, 0.402963, -0.216791, -0.68954),
      (0.754349, -0.469728, -0.0529346, 0.128788, -0.421497),
      (0.313066, 0.53096, 0.0968626, 0.649668, -0.870967),
      (0.634037, -0.576122, 0.0550574, -0.924834, -0.300604))
    ProposalTarget.BBOX_NORMALIZE_TARGETS_PRECOMPUTED = false
    val targets = proposalTarget.computeTargets(Tensor(convert(exRois, Float)),
      Tensor(convert(gtRois, Float)), Tensor(Storage(labels)))

    assertMatrixEqualTM(targets, expected, 1e-4)
  }

  it should "computeTargets with norm correcly" in {
    val expected = DenseMatrix((0.508699, 2.02244, -1.5083, -0.242714, -6.94869),
      (0.0884602, 0.911369, -0.446058, -0.331711, -4.40635),
      (0.528035, 0.663603, 7.51411, -0.190237, 2.43739),
      (0.992158, 1.49501, -0.39554, -0.192576, -4.62689),
      (0.395036, -6.99306, 1.34789, -0.7375, 0.699932),
      (0.335596, -0.877232, -6.82606, -0.501458, 1.63962),
      (0.805451, 8.16015, 4.02963, -1.08396, -3.4477),
      (0.754349, -4.69728, -0.529346, 0.643939, -2.10748),
      (0.313066, 5.3096, 0.968626, 3.24834, -4.35484),
      (0.634037, -5.76122, 0.550574, -4.62417, -1.50302))

    ProposalTarget.BBOX_NORMALIZE_TARGETS_PRECOMPUTED = true

    val targets = proposalTarget.computeTargets(Tensor(convert(exRois, Float)),
      Tensor(convert(gtRois, Float)), Tensor(Storage(labels)))

    assertMatrixEqualTM(targets, expected, 1e-4)
  }

  def assertMatrixEqualTM(actual: Tensor[Float],
    expected: DenseMatrix[Double], diff: Double): Unit = {
    if (actual.dim() == 1) {
      assert(actual.nElement() == expected.size)
      var d = 1
      for (r <- 0 until expected.rows) {
        for (c <- 0 until expected.cols) {
          assert(abs(expected(r, c) - actual.valueAt(d)) < diff)
          d += 1
        }
      }
    } else {
      assert(actual.size(1) == expected.rows && actual.size(2) == expected.cols)
      for (r <- 0 until expected.rows) {
        for (c <- 0 until expected.cols) {
          assert(abs(expected(r, c) - actual.valueAt(r + 1, c + 1)) < diff)
        }
      }
    }
  }

  it should "getBboxRegressionLabels" in {
    val data = DenseMatrix((1, 14, 2, 17, 16),
      (1, 15, 4, 11, 16),
      (4, 9, 2, 12, 4),
      (1, 1, 13, 19, 4),
      (3, 4, 3, 7, 17),
      (5, 15, 1, 14, 7),
      (3, 16, 2, 9, 19),
      (6, 2, 14, 17, 16),
      (3, 15, 7, 13, 6),
      (3, 12, 18, 0, 2))

    val (r1, r2) = BboxUtil.getBboxRegressionLabels(Tensor(convert(data, Float)), 6)

    val expected1 = DenseMatrix(
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 16.0, 0.0, 15.0, 12.0),
      (0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 2.0, 0.0, 7.0, 18.0),
      (0.0, 0.0, 0.0, 0.0, 7.0, 0.0, 9.0, 0.0, 13.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 17.0, 0.0, 19.0, 0.0, 6.0, 2.0),
      (0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 14.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 17.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 16.0, 0.0, 0.0)).t

    val expected2 = DenseMatrix(
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0),
      (0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0),
      (0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0),
      (0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0),
      (0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0)).t

    assert(r1.size(1) == expected1.rows && r1.size(2) == expected1.cols)
    for (r <- 0 until expected1.rows) {
      for (c <- 0 until expected1.cols) {
        assert(abs(expected1(r, c) - r1.valueAt(r + 1, c + 1)) < 1e-6)
      }
    }

    assert(r2.size(1) == expected2.rows && r2.size(2) == expected2.cols)
    for (r <- 0 until expected2.rows) {
      for (c <- 0 until expected2.cols) {
        assert(abs(expected2(r, c) - r2.valueAt(r + 1, c + 1)) < 1e-6)
      }
    }
  }

  "proposal target" should "work properly" in {
    val layer = ProposalTarget(128, 21)
    layer.setDebug(true)
    val rpnRois = Tensor(Storage(Array(0.0, 0.0, 140.13168335, 195.587539673, 203.678405762,
      0.0, 10.1558227539, 122.821655273, 289.163665771, 155.377593994,
      0.0, 0.0, 162.800170898, 155.978851318, 199.270751953,
      0.0, 0.0, 142.478027344, 185.672363281, 227.153381348,
      0.0, 433.291992188, 284.422332764, 600.0, 399.0,
      0.0, 0.0, 319.659332275, 225.634033203, 399.0,
      0.0, 0.0, 87.2241516113, 171.026199341, 355.218780518,
      0.0, 0.0, 170.629989624, 237.602813721, 249.537155151,
      0.0, 345.528442383, 279.131347656, 458.900634766, 394.383178711,
      0.0, 157.04246521, 367.226593018, 258.294372559, 399.0,
      0.0, 116.368011475, 329.589294434, 269.162628174, 399.0,
      0.0, 501.133544922, 195.494262695, 590.040893555, 378.067993164,
      0.0, 281.055786133, 164.420593262, 459.83190918, 399.0,
      0.0, 0.0, 373.666259766, 251.2628479, 399.0,
      0.0, 514.305236816, 213.669067383, 600.0, 399.0,
      0.0, 0.0, 203.587524414, 248.7996521, 256.385803223,
      0.0, 525.835266113, 277.469360352, 600.0, 391.182800293,
      0.0, 71.4769668579, 93.9724807739, 294.459075928, 124.651023865,
      0.0, 0.0, 178.168029785, 181.528320312, 264.571075439,
      0.0, 353.979125977, 250.448638916, 408.485168457, 349.280670166,
      0.0, 553.83605957, 228.324356079, 600.0, 399.0,
      0.0, 188.910858154, 306.034240723, 334.223297119, 399.0,
      0.0, 360.093017578, 0.0, 395.223144531, 129.777404785,
      0.0, 157.14642334, 326.654724121, 261.762481689, 399.0,
      0.0, 237.221343994, 0.0, 496.184844971, 140.99230957,
      0.0, 490.963684082, 81.589515686, 600.0, 263.916748047,
      0.0, 11.8044204712, 0.0, 234.120056152, 112.362884521,
      0.0, 0.0, 0.0, 117.274620056, 264.733398438,
      0.0, 0.0, 0.0, 104.011749268, 129.672134399,
      0.0, 470.449493408, 266.737976074, 546.08392334, 399.0).map(_.toFloat))).resize(30, 5)
    val gts = Tensor(Storage(Array(0, 14 + 1, 0, 270.270263672,
      157.35736084, 349.549560547, 265.465454102,
      0, 15 + 1, 0, 272.672668457, 127.327323914,
      337.537536621, 223.423416138,
      0, 15 + 1, 0, 52.8528518677, 333.933929443,
      76.8768768311, 391.591583252).map(_.toFloat))).resize(3, 7)
    val input = T(rpnRois, gts)
    layer.forward(input)

    val expectedRois = Tensor(Storage(Array(
      0.0, 270.270263672, 157.35736084, 349.549560547, 265.465454102,
      0.0, 272.672668457, 127.327323914, 337.537536621, 223.423416138,
      0.0, 52.8528518677, 333.933929443, 76.8768768311, 391.591583252,
      0.0, 0.0, 140.13168335, 195.587539673, 203.678405762,
      0.0, 10.1558227539, 122.821655273, 289.163665771, 155.377593994,
      0.0, 0.0, 162.800170898, 155.978851318, 199.270751953,
      0.0, 0.0, 142.478027344, 185.672363281, 227.153381348,
      0.0, 433.291992188, 284.422332764, 600.0, 399.0,
      0.0, 0.0, 319.659332275, 225.634033203, 399.0,
      0.0, 0.0, 87.2241516113, 171.026199341, 355.218780518,
      0.0, 0.0, 170.629989624, 237.602813721, 249.537155151,
      0.0, 345.528442383, 279.131347656, 458.900634766, 394.383178711,
      0.0, 157.04246521, 367.226593018, 258.294372559, 399.0,
      0.0, 116.368011475, 329.589294434, 269.162628174, 399.0,
      0.0, 501.133544922, 195.494262695, 590.040893555, 378.067993164,
      0.0, 281.055786133, 164.420593262, 459.83190918, 399.0,
      0.0, 0.0, 373.666259766, 251.2628479, 399.0,
      0.0, 514.305236816, 213.669067383, 600.0, 399.0,
      0.0, 0.0, 203.587524414, 248.7996521, 256.385803223,
      0.0, 525.835266113, 277.469360352, 600.0, 391.182800293,
      0.0, 71.4769668579, 93.9724807739, 294.459075928, 124.651023865,
      0.0, 0.0, 178.168029785, 181.528320312, 264.571075439,
      0.0, 353.979125977, 250.448638916, 408.485168457, 349.280670166,
      0.0, 553.83605957, 228.324356079, 600.0, 399.0,
      0.0, 188.910858154, 306.034240723, 334.223297119, 399.0,
      0.0, 360.093017578, 0.0, 395.223144531, 129.777404785,
      0.0, 157.14642334, 326.654724121, 261.762481689, 399.0,
      0.0, 237.221343994, 0.0, 496.184844971, 140.99230957,
      0.0, 490.963684082, 81.589515686, 600.0, 263.916748047,
      0.0, 11.8044204712, 0.0, 234.120056152, 112.362884521,
      0.0, 0.0, 0.0, 117.274620056, 264.733398438,
      0.0, 0.0, 0.0, 104.011749268, 129.672134399,
      0.0, 470.449493408, 266.737976074, 546.08392334, 399.0).map(_.toFloat))).resize(33, 5)
    layer.output[Tensor[Float]](1) should be(expectedRois)

    val labels = layer.output[Tensor[Float]](2)
    labels.valueAt(1) should be(15)
    labels.valueAt(2) should be(16)
    labels.valueAt(3) should be(16)

    layer.output[Tensor[Float]](3).abs().sum() should be(0)
    val insideWeight = layer.output[Tensor[Float]](4)
    insideWeight.valueAt(1, 57) should be(1)
    insideWeight.valueAt(1, 58) should be(1)
    insideWeight.valueAt(1, 59) should be(1)
    insideWeight.valueAt(1, 60) should be(1)

    insideWeight.valueAt(2, 61) should be(1)
    insideWeight.valueAt(2, 62) should be(1)
    insideWeight.valueAt(2, 63) should be(1)
    insideWeight.valueAt(2, 64) should be(1)

    insideWeight.valueAt(3, 61) should be(1)
    insideWeight.valueAt(3, 62) should be(1)
    insideWeight.valueAt(3, 63) should be(1)
    insideWeight.valueAt(3, 64) should be(1)

  }

  "ProposalTarget serializer" should "work properly" in {
    val layer = ProposalTarget(256, 21)
    val layer2 = ProposalTarget(256, 21)
    val rpnRois = Tensor(Storage(Array(0.0, 0.0, 140.13168335, 195.587539673, 203.678405762,
      0.0, 10.1558227539, 122.821655273, 289.163665771, 155.377593994,
      0.0, 0.0, 162.800170898, 155.978851318, 199.270751953,
      0.0, 0.0, 142.478027344, 185.672363281, 227.153381348,
      0.0, 433.291992188, 284.422332764, 600.0, 399.0,
      0.0, 0.0, 319.659332275, 225.634033203, 399.0,
      0.0, 0.0, 87.2241516113, 171.026199341, 355.218780518,
      0.0, 0.0, 170.629989624, 237.602813721, 249.537155151,
      0.0, 345.528442383, 279.131347656, 458.900634766, 394.383178711,
      0.0, 157.04246521, 367.226593018, 258.294372559, 399.0,
      0.0, 116.368011475, 329.589294434, 269.162628174, 399.0,
      0.0, 501.133544922, 195.494262695, 590.040893555, 378.067993164,
      0.0, 281.055786133, 164.420593262, 459.83190918, 399.0,
      0.0, 0.0, 373.666259766, 251.2628479, 399.0,
      0.0, 514.305236816, 213.669067383, 600.0, 399.0,
      0.0, 0.0, 203.587524414, 248.7996521, 256.385803223,
      0.0, 525.835266113, 277.469360352, 600.0, 391.182800293,
      0.0, 71.4769668579, 93.9724807739, 294.459075928, 124.651023865,
      0.0, 0.0, 178.168029785, 181.528320312, 264.571075439,
      0.0, 353.979125977, 250.448638916, 408.485168457, 349.280670166,
      0.0, 553.83605957, 228.324356079, 600.0, 399.0,
      0.0, 188.910858154, 306.034240723, 334.223297119, 399.0,
      0.0, 360.093017578, 0.0, 395.223144531, 129.777404785,
      0.0, 157.14642334, 326.654724121, 261.762481689, 399.0,
      0.0, 237.221343994, 0.0, 496.184844971, 140.99230957,
      0.0, 490.963684082, 81.589515686, 600.0, 263.916748047,
      0.0, 11.8044204712, 0.0, 234.120056152, 112.362884521,
      0.0, 0.0, 0.0, 117.274620056, 264.733398438,
      0.0, 0.0, 0.0, 104.011749268, 129.672134399,
      0.0, 470.449493408, 266.737976074, 546.08392334, 399.0).map(_.toFloat))).resize(30, 5)
    val gts = Tensor(Storage(Array(0, 14 + 1, 0, 270.270263672,
      157.35736084, 349.549560547, 265.465454102,
      0, 15 + 1, 0, 272.672668457, 127.327323914,
      337.537536621, 223.423416138,
      0, 15 + 1, 0, 52.8528518677, 333.933929443,
      76.8768768311, 391.591583252).map(_.toFloat))).resize(3, 7)
    val input = T(rpnRois, gts)
    layer.setDebug(true)
    layer2.setDebug(true)
    val res1 = layer.forward(input)
    val tmpFile = java.io.File.createTempFile("module", ".bigdl")
    layer.saveModule(tmpFile.getAbsolutePath, overWrite = true)
    val loaded = Module.loadModule[Float](tmpFile.getAbsolutePath)
    val res2 = layer2.forward(input).toTable
    res1 should be(res2)
    if (tmpFile.exists()) {
      tmpFile.delete()
    }
  }
}


